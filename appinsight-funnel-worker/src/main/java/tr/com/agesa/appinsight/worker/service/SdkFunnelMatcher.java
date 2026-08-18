package tr.com.agesa.appinsight.worker.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * {@code workers/funnelMatcher.ts → evaluateSdkFunnel} durum makinesinin karşılığı.
 *
 * <p>Kararı üretir, <b>uygulamaz</b> — insightEngine Faz 4'te gelecek (bkz. {@link FunnelDecision}).
 *
 * <h2>Node'daki akış sırası (birebir korunuyor, sıra sonucu değiştirir)</h2>
 * <ol>
 *   <li>Durum var ve tamamlanmamışsa <b>global timeout</b> kontrolü — dolmuşsa timeout kararı,
 *       durum silinir ve akış "durum yokmuş gibi" devam eder (Node'da {@code state = null}).</li>
 *   <li>Durum yoksa: ilk adım eşleşiyor mu? Tek adımlıysa dedup'a bakıp <b>tamamla</b>,
 *       çok adımlıysa durumu oluştur.</li>
 *   <li>Durum {@code completed} işaretliyse durum silinir, karar üretilmez.</li>
 *   <li><b>Adım timeout'u</b> ({@code step.timeoutMs ?? globalTimeoutMs}) kontrolü.</li>
 *   <li>Sıradaki adım yoksa "crash recovery" tamamlanması.</li>
 *   <li>Sıradaki adım GCL ise beklenir; SDK ise eşleşme + süre koşulu aranır.</li>
 * </ol>
 */
@Service
public class SdkFunnelMatcher {

    /** Node: session_once modunda dedup anahtarı 24 saat yaşar. */
    private static final long SESSION_ONCE_TTL_SEC = 86_400;

    private final StepMatcher stepMatcher;
    private final FunnelStateStore stateStore;

    public SdkFunnelMatcher(StepMatcher stepMatcher, FunnelStateStore stateStore) {
        this.stepMatcher = stepMatcher;
        this.stateStore = stateStore;
    }

    /**
     * Bir funnel'ı tek bir ekran eventine karşı değerlendirir.
     *
     * @param funnel  {@code steps}, {@code globalTimeoutMs}, {@code triggerMode} içeren tanım
     * @param screen  <b>kanonikleştirilmiş</b> ekran adı (ham ad değil)
     */
    @SuppressWarnings("unchecked")
    public FunnelDecision evaluate(Map<String, Object> funnel,
                                   String deviceId,
                                   String sessionId,
                                   String screen,
                                   String eventType,
                                   long ts,
                                   long durationMs) {

        String funnelId = String.valueOf(funnel.get("id"));
        List<Map<String, Object>> steps = (List<Map<String, Object>>) funnel.get("steps");
        if (steps == null || steps.isEmpty()) {
            return FunnelDecision.none();
        }
        long globalTimeoutMs = number(funnel.get("globalTimeoutMs"), 1_800_000);
        String triggerMode = funnel.get("triggerMode") == null ? "session_once" : String.valueOf(funnel.get("triggerMode"));

        String stateKey = stateStore.stateKey(funnelId, deviceId);
        FunnelState state = stateStore.load(stateKey);

        // 1) Global timeout — durum silinir ve akış "durum yokmuş gibi" DEVAM eder.
        FunnelDecision timeoutDecision = null;
        if (state != null && !state.completed() && ts - state.startedAt() > globalTimeoutMs) {
            timeoutDecision = new FunnelDecision(FunnelDecision.Type.FUNNEL_TIMEOUT, funnelId, deviceId, screen,
                    state.currentStep(), steps.size(), ts, "global_timeout");
            stateStore.delete(stateKey);
            state = null;
        }

        // 2) Durum yok — ilk adım denemesi
        if (state == null) {
            FunnelDecision started = tryStart(funnelId, steps, globalTimeoutMs, triggerMode,
                    deviceId, sessionId, screen, eventType, ts, durationMs, stateKey);
            // Node aynı çağrıda önce timeout'u işleyip sonra return ediyor; timeout varsa
            // o karar baskındır (başlatma denemesi yine yapılır ama karar olarak timeout döner).
            return timeoutDecision != null ? timeoutDecision : started;
        }

        // 3) Tamamlanmış durum — temizle, karar yok
        if (state.completed()) {
            stateStore.delete(stateKey);
            return FunnelDecision.none();
        }

        // 4) Adım timeout'u
        Map<String, Object> currentStepDef = steps.get(state.currentStep() - 1);
        long stepTimeout = number(currentStepDef.get("timeoutMs"), globalTimeoutMs);
        if (ts - state.stepEnteredAt() > stepTimeout) {
            stateStore.delete(stateKey);
            return new FunnelDecision(FunnelDecision.Type.FUNNEL_TIMEOUT, funnelId, deviceId, screen,
                    state.currentStep(), steps.size(), ts, "step_timeout");
        }

        // 5) Sıradaki adım yok — yalnızca BOZUK durumlarda (ör. funnel adımları kısaltıldığında)
        //    tetiklenir. Düzeltmeden önce normal tamamlanma yolu buydu; artık değil.
        if (state.currentStep() >= steps.size()) {
            stateStore.delete(stateKey);
            return new FunnelDecision(FunnelDecision.Type.FUNNEL_COMPLETED, funnelId, deviceId, screen,
                    steps.size(), steps.size(), ts, "crash_recovery");
        }

        Map<String, Object> nextStep = steps.get(state.currentStep());

        // 6) GCL adımı — SDK matcher dokunmaz, GCL matcher ilerletir
        if ("gcl".equals(nextStep.get("source"))) {
            return FunnelDecision.none();
        }
        if (!stepMatcher.stepMatches(nextStep, screen, eventType)
                || !stepMatcher.minDurationSatisfied(nextStep, durationMs)) {
            return FunnelDecision.none();
        }

        FunnelState advanced = state.advancedTo(state.currentStep() + 1, ts);

        // `>` DEĞİL `>=` — currentStep "tamamlanan adım sayısı"dır; N adımlı funnel'da son adım
        // eşleştiğinde currentStep === N olur. Node'da bu koşul `>` idi ve dal hiç çalışmıyordu
        // (yukarıdaki crash-recovery dalı aynı durumu önce yakalıyordu). Hata iki sistemde
        // birlikte düzeltildi; bkz. docs/BACKLOG.md.
        if (advanced.currentStep() >= steps.size()) {
            // session_once: aynı oturumda ikinci kez tetiklenmesin
            if ("session_once".equals(triggerMode)) {
                String visitKey = stateStore.visitKey(funnelId, deviceId, sessionId);
                if (stateStore.exists(visitKey)) {
                    stateStore.delete(stateKey);
                    return new FunnelDecision(FunnelDecision.Type.SKIPPED_DEDUP, funnelId, deviceId, screen,
                            steps.size(), steps.size(), ts, "visit_fired");
                }
                stateStore.markVisitFired(visitKey, SESSION_ONCE_TTL_SEC);
            }
            stateStore.delete(stateKey);
            return new FunnelDecision(FunnelDecision.Type.FUNNEL_COMPLETED, funnelId, deviceId, screen,
                    steps.size(), steps.size(), ts, "funnel_completed");
        }

        stateStore.save(stateKey, advanced, globalTimeoutMs);
        return new FunnelDecision(FunnelDecision.Type.STEP_ADVANCED, funnelId, deviceId, screen,
                advanced.currentStep(), steps.size(), ts, null);
    }

    /** Durum yokken ilk adımı dener: tek adımlıysa tamamlar, çok adımlıysa durumu kurar. */
    private FunnelDecision tryStart(String funnelId,
                                    List<Map<String, Object>> steps,
                                    long globalTimeoutMs,
                                    String triggerMode,
                                    String deviceId,
                                    String sessionId,
                                    String screen,
                                    String eventType,
                                    long ts,
                                    long durationMs,
                                    String stateKey) {

        Map<String, Object> firstStep = steps.get(0);
        // GCL ile başlayan funnel'ı SDK matcher başlatmaz
        if ("gcl".equals(firstStep.get("source"))) {
            return FunnelDecision.none();
        }
        if (!stepMatcher.stepMatches(firstStep, screen, eventType)
                || !stepMatcher.minDurationSatisfied(firstStep, durationMs)) {
            return FunnelDecision.none();
        }

        if (steps.size() > 1) {
            stateStore.save(stateKey,
                    new FunnelState(funnelId, deviceId, 1, ts, ts, false, null), globalTimeoutMs);
            return new FunnelDecision(FunnelDecision.Type.FUNNEL_STARTED, funnelId, deviceId, screen,
                    1, steps.size(), ts, null);
        }

        // Tek adımlı funnel: durum tutulmaz, doğrudan tamamlanır.
        // Dedup anahtarı trigger moduna göre DEĞİŞİR:
        //   session_once → oturum bazlı (…:{deviceId}:{sessionId}), TTL 24h
        //   screen_visit → cihaz bazlı  (…:{deviceId}),             TTL globalTimeout/1000 + 60
        boolean sessionOnce = "session_once".equals(triggerMode);
        String visitKey = stateStore.visitKey(funnelId, deviceId, sessionOnce ? sessionId : null);
        if (stateStore.exists(visitKey)) {
            return new FunnelDecision(FunnelDecision.Type.SKIPPED_DEDUP, funnelId, deviceId, screen,
                    1, 1, ts, "visit_fired");
        }

        long ttlSec = sessionOnce ? SESSION_ONCE_TTL_SEC : (long) Math.ceil(globalTimeoutMs / 1000.0) + 60;
        stateStore.markVisitFired(visitKey, ttlSec);
        return new FunnelDecision(FunnelDecision.Type.FUNNEL_COMPLETED, funnelId, deviceId, screen,
                1, 1, ts, "single_step");
    }

    private static long number(Object value, long fallback) {
        return value instanceof Number n ? n.longValue() : fallback;
    }
}
