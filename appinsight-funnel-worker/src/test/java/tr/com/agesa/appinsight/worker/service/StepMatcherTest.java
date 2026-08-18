package tr.com.agesa.appinsight.worker.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * funnelMatcher karar fonksiyonları parite testi.
 *
 * <p>Beklenen değerler Node'daki {@code funnelMatcher.ts} fonksiyonları canlı çalıştırılıp
 * alındı — uydurulmadı.
 */
class StepMatcherTest {

    private final StepMatcher matcher = new StepMatcher(new FilterEvaluator());

    private static Map<String, Object> step(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    private static Map<String, String> event() {
        Map<String, String> e = new LinkedHashMap<>();
        e.put("device_id", "D1");
        e.put("platform", "ios");
        e.put("app_version", "3.6.0");
        e.put("os_version", "18.1");
        e.put("model", "iPhone16,2");
        return e;
    }

    @Test
    @DisplayName("ekran eşleştirme — exact / prefix / regex ve bozuk tanımlar")
    void ekranEslestirme() {
        assertThat(matcher.screenMatches(step("screen", "HomeViewController", "matchType", "exact"), "HomeViewController")).isTrue();
        assertThat(matcher.screenMatches(step("screen", "HomeViewController", "matchType", "exact"), "HomeView")).isFalse();
        assertThat(matcher.screenMatches(step("screen", "Home", "matchType", "prefix"), "HomeViewController")).isTrue();
        assertThat(matcher.screenMatches(step("screen", "Home", "matchType", "prefix"), "MyHomeViewController")).isFalse();
        assertThat(matcher.screenMatches(step("screen", "^Home.*", "matchType", "regex"), "HomeViewController")).isTrue();
        // regex KISMİ eşleşme arar (JS .test gibi), tam eşleşme değil
        assertThat(matcher.screenMatches(step("screen", "View", "matchType", "regex"), "HomeViewController")).isTrue();
        // bozuk regex sessizce false
        assertThat(matcher.screenMatches(step("screen", "[bozuk(", "matchType", "regex"), "HomeViewController")).isFalse();
        // eksik tanımlı adım hiçbir şeyle eşleşmez
        assertThat(matcher.screenMatches(step("screen", "Home", "matchType", ""), "Home")).isFalse();
        assertThat(matcher.screenMatches(step("screen", "", "matchType", "exact"), "")).isFalse();
        // tanınmayan matchType → tam eşitlik (Node'daki default dalı)
        assertThat(matcher.screenMatches(step("screen", "Home", "matchType", "bilinmeyen"), "Home")).isTrue();
    }

    @Test
    @DisplayName("adım eşleştirme — eventType koşulu")
    void adimEslestirme() {
        Map<String, Object> withEventType = step("screen", "Home", "matchType", "exact",
                "conditions", Map.of("eventType", "appeared"));
        assertThat(matcher.stepMatches(withEventType, "Home", "appeared")).isTrue();
        assertThat(matcher.stepMatches(withEventType, "Home", "dwell")).isFalse();

        assertThat(matcher.stepMatches(step("screen", "Home", "matchType", "exact", "conditions", Map.of()),
                "Home", "dwell")).isTrue();
        assertThat(matcher.stepMatches(step("screen", "Home", "matchType", "exact"),
                "Home", "disappeared")).isTrue();
    }

    @Test
    @DisplayName("minDurationMs — 0 ve eksik koşul 'kontrol yok' sayılır")
    void minSureKosulu() {
        Map<String, Object> min3s = step("conditions", Map.of("minDurationMs", 3000));
        assertThat(matcher.minDurationSatisfied(min3s, 2999)).isFalse();
        assertThat(matcher.minDurationSatisfied(min3s, 3000)).isTrue();

        // Node: `if (!min) return true` — 0 da "koşul yok" demek
        assertThat(matcher.minDurationSatisfied(step("conditions", Map.of("minDurationMs", 0)), 0)).isTrue();
        assertThat(matcher.minDurationSatisfied(step("conditions", Map.of()), 0)).isTrue();
        assertThat(matcher.minDurationSatisfied(step(), 5000)).isTrue();
    }

    @Test
    @DisplayName("hedef filtresi — deviceIds, platform, sürüm eşitliği")
    void hedefFiltresi() {
        assertThat(matcher.targetFilterMatches(null, event())).isTrue();
        assertThat(matcher.targetFilterMatches(Map.of(), event())).isTrue();
        assertThat(matcher.targetFilterMatches(Map.of("platform", "ios"), event())).isTrue();
        assertThat(matcher.targetFilterMatches(Map.of("platform", "android"), event())).isFalse();
        assertThat(matcher.targetFilterMatches(Map.of("deviceIds", List.of("D1")), event())).isTrue();
        assertThat(matcher.targetFilterMatches(Map.of("deviceIds", List.of("D2")), event())).isFalse();
        // boş liste = filtre yok
        assertThat(matcher.targetFilterMatches(Map.of("deviceIds", List.of()), event())).isTrue();
        assertThat(matcher.targetFilterMatches(Map.of("appVersion", "3.6.0"), event())).isTrue();
        assertThat(matcher.targetFilterMatches(Map.of("appVersion", "3.5.0"), event())).isFalse();
        assertThat(matcher.targetFilterMatches(Map.of("osVersion", "18.1"), event())).isTrue();
    }

    @Test
    @DisplayName("filterExpression varsa appVersion/osVersion alanları ARTIK okunmaz")
    void filtreIfadesiDigerAlanlariGolgeler() {
        assertThat(matcher.targetFilterMatches(
                Map.of("filterExpression", "appVersion >= \"3.0.0\""), event())).isTrue();
        assertThat(matcher.targetFilterMatches(
                Map.of("filterExpression", "appVersion >= \"9.9.9\""), event())).isFalse();

        // Node ifadeyi değerlendirip HEMEN dönüyor; eşleşmeyen appVersion hiç bakılmıyor.
        // Portalde ikisi birden doldurulursa sürüm alanı sessizce yok sayılır.
        assertThat(matcher.targetFilterMatches(
                Map.of("filterExpression", "appVersion >= \"3.0.0\"", "appVersion", "ESLESMEZ"),
                event())).isTrue();

        // platform kısayolu ifadeden ÖNCE çalışır, ikisi birlikte kullanılabilir
        assertThat(matcher.targetFilterMatches(
                Map.of("platform", "ios", "filterExpression", "platform = \"ios\""), event())).isTrue();
    }

    @Test
    @DisplayName("cihaz bağlamı snake_case ve camelCase anahtarları birlikte okur")
    void cihazBaglami() {
        Map<String, String> camel = new LinkedHashMap<>();
        camel.put("platform", "android");
        camel.put("appVersion", "2.0.0");
        camel.put("osVersion", "14");

        assertThat(matcher.deviceContext(camel))
                .containsEntry("platform", "android")
                .containsEntry("appVersion", "2.0.0")
                .containsEntry("osVersion", "14")
                .containsEntry("model", "");
    }
}
