package tr.com.agesa.appinsight.worker.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * funnelMatcher'ın <b>saf karar fonksiyonları</b> — ekran eşleştirme, süre koşulu ve
 * hedef filtresi.
 *
 * <p>Durum makinesinin kendisi (Redis {@code funnel_state}, adım ilerlemesi, timeout)
 * ayrı gelecek; burası motorun yan etkisiz çekirdeği olduğu için Node'a karşı doğrudan
 * diferansiyel test edilebiliyor.
 *
 * <p>Karşılığı: {@code workers/funnelMatcher.ts} → {@code sdkScreenMatches},
 * {@code sdkStepMatches}, {@code checkMinDuration}, {@code deviceCtx}, {@code targetFilterMatches}.
 */
@Component
public class StepMatcher {

    private final FilterEvaluator filterEvaluator;

    public StepMatcher(FilterEvaluator filterEvaluator) {
        this.filterEvaluator = filterEvaluator;
    }

    /**
     * Ekran adı adımla eşleşiyor mu?
     *
     * <p>{@code screen} veya {@code matchType} boşsa <b>false</b> — eksik tanımlı adım
     * hiçbir şeyle eşleşmez (Node'da da öyle; "her şeyi geçir" değil).
     *
     * <p>Geçersiz regex sessizce false döner.
     */
    public boolean screenMatches(Map<String, Object> step, String screen) {
        String stepScreen = str(step.get("screen"));
        String matchType = str(step.get("matchType"));
        if (isBlank(stepScreen) || isBlank(matchType)) {
            return false;
        }
        String target = screen == null ? "" : screen;

        return switch (matchType) {
            case "prefix" -> target.startsWith(stepScreen);
            case "regex" -> regexMatches(stepScreen, target);
            // 'exact' ve tanınmayan matchType: tam eşitlik (Node'daki default dalı)
            default -> target.equals(stepScreen);
        };
    }

    /** Ekran eşleşmesi + varsa {@code conditions.eventType} kontrolü. */
    public boolean stepMatches(Map<String, Object> step, String screen, String eventType) {
        String stepScreen = str(step.get("screen"));
        String matchType = str(step.get("matchType"));
        if (isBlank(stepScreen) || isBlank(matchType)) {
            return false;
        }
        String requiredEventType = str(conditions(step).get("eventType"));
        if (!isBlank(requiredEventType) && !requiredEventType.equals(eventType)) {
            return false;
        }
        return screenMatches(step, screen);
    }

    /**
     * Minimum süre koşulu. Koşul yoksa (veya 0 ise) her zaman geçer —
     * Node'daki {@code if (!min) return true} ifadesi 0'ı da "koşul yok" sayıyor.
     */
    public boolean minDurationSatisfied(Map<String, Object> step, long durationMs) {
        Object min = conditions(step).get("minDurationMs");
        if (!(min instanceof Number n) || n.longValue() == 0) {
            return true;
        }
        return durationMs >= n.longValue();
    }

    /**
     * Cihaz bağlamı — filtre ifadesinde kullanılan alanlar.
     * Event hem snake_case hem camelCase anahtar taşıyabiliyor; ikisi de destekleniyor.
     */
    public Map<String, String> deviceContext(Map<String, String> event) {
        return Map.of(
                "platform", orEmpty(event.get("platform")),
                "appVersion", firstNonEmpty(event.get("app_version"), event.get("appVersion")),
                "osVersion", firstNonEmpty(event.get("os_version"), event.get("osVersion")),
                "model", orEmpty(event.get("model")));
    }

    /**
     * Funnel hedef filtresi.
     *
     * <p>Node'daki sıra <b>aynen</b> korunuyor, çünkü sıra sonucu değiştiriyor:
     * <ol>
     *   <li>{@code deviceIds} listesi doluysa cihaz listede değilse eler</li>
     *   <li>{@code platform} kısayolu (ifade gerektirmez)</li>
     *   <li>{@code filterExpression} varsa <b>burada döner</b> — aşağıdaki
     *       {@code appVersion}/{@code osVersion} kontrolleri ARTIK ÇALIŞMAZ</li>
     *   <li>ifade yoksa {@code appVersion} / {@code osVersion} tam eşitlik</li>
     * </ol>
     */
    @SuppressWarnings("unchecked")
    public boolean targetFilterMatches(Map<String, Object> targetFilter, Map<String, String> event) {
        if (targetFilter == null || targetFilter.isEmpty()) {
            return true;
        }

        Object deviceIds = targetFilter.get("deviceIds");
        if (deviceIds instanceof List<?> ids && !ids.isEmpty() && !ids.contains(event.get("device_id"))) {
            return false;
        }

        String platform = str(targetFilter.get("platform"));
        if (!isBlank(platform) && !platform.equals(orEmpty(event.get("platform")))) {
            return false;
        }

        String expression = str(targetFilter.get("filterExpression"));
        if (!isBlank(expression)) {
            return filterEvaluator.evaluate(expression, deviceContext(event));
        }

        String appVersion = str(targetFilter.get("appVersion"));
        if (!isBlank(appVersion)
                && !appVersion.equals(firstNonEmpty(event.get("app_version"), event.get("appVersion")))) {
            return false;
        }
        String osVersion = str(targetFilter.get("osVersion"));
        if (!isBlank(osVersion)
                && !osVersion.equals(firstNonEmpty(event.get("os_version"), event.get("osVersion")))) {
            return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> conditions(Map<String, Object> step) {
        Object conditions = step.get("conditions");
        return conditions instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private static boolean regexMatches(String pattern, String input) {
        try {
            // Node: new RegExp(p).test(s) — kısmi eşleşme arar, tam eşleşme değil.
            return Pattern.compile(pattern).matcher(input).find();
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    private static String str(Object value) {
        return value instanceof String s ? s : (value == null ? null : String.valueOf(value));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isEmpty();
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    /** Node: {@code a ?? b ?? ''} — null/undefined atlanır, boş string DEĞER sayılır. */
    private static String firstNonEmpty(String first, String second) {
        if (first != null) {
            return first;
        }
        return second == null ? "" : second;
    }
}
