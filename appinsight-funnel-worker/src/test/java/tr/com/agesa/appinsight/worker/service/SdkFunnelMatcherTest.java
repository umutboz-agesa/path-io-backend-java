package tr.com.agesa.appinsight.worker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tr.com.agesa.appinsight.worker.config.WorkerProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Durum makinesi testleri — Redis yerine bellek içi sahte depo kullanılıyor,
 * böylece adım geçişleri ve dedup davranışı Redis olmadan doğrulanabiliyor.
 */
class SdkFunnelMatcherTest {

    /** {@link FunnelStateStore}'un bellek içi ikizi — gerçek anahtar biçimlerini üretir. */
    static class FakeStore extends FunnelStateStore {
        final Map<String, FunnelState> states = new HashMap<>();
        final Set<String> visitKeys = new HashSet<>();

        FakeStore() {
            // Gölge ön eki testte önemsiz; anahtar biçimleri zaten override ediliyor.
            super(null, null, new WorkerProperties(List.of(), null, null, false, 0));
        }

        @Override
        public String stateKey(String funnelId, String deviceId) {
            return "funnel_state:" + funnelId + ":" + deviceId;
        }

        @Override
        public String visitKey(String funnelId, String deviceId, String sessionId) {
            return sessionId == null
                    ? "funnel_visit_fired:" + funnelId + ":" + deviceId
                    : "funnel_visit_fired:" + funnelId + ":" + deviceId + ":" + sessionId;
        }

        @Override
        public FunnelState load(String key) {
            return states.get(key);
        }

        @Override
        public void save(String key, FunnelState state, long globalTimeoutMs) {
            states.put(key, state);
        }

        @Override
        public void delete(String key) {
            states.remove(key);
        }

        @Override
        public boolean exists(String key) {
            return visitKeys.contains(key);
        }

        @Override
        public void markVisitFired(String key, long ttlSeconds) {
            visitKeys.add(key);
        }
    }

    private FakeStore store;
    private SdkFunnelMatcher matcher;

    @BeforeEach
    void setUp() {
        store = new FakeStore();
        matcher = new SdkFunnelMatcher(new StepMatcher(new FilterEvaluator()), store);
    }

    private static Map<String, Object> step(String screen, Object... extra) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("screen", screen);
        s.put("matchType", "exact");
        s.put("source", "sdk");
        for (int i = 0; i < extra.length; i += 2) {
            s.put((String) extra[i], extra[i + 1]);
        }
        return s;
    }

    private static Map<String, Object> funnel(String triggerMode, List<Map<String, Object>> steps) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("id", "F1");
        f.put("steps", steps);
        f.put("globalTimeoutMs", 300_000);
        f.put("triggerMode", triggerMode);
        return f;
    }

    @Test
    @DisplayName("çok adımlı funnel SON adım eşleştiğinde tamamlanır")
    void cokAdimliAkis() {
        Map<String, Object> f = funnel("session_once",
                new ArrayList<>(List.of(step("Home"), step("Cart"), step("Checkout"))));

        assertThat(matcher.evaluate(f, "D1", "S1", "Home", "appeared", 1000, 0).type())
                .isEqualTo(FunnelDecision.Type.FUNNEL_STARTED);
        assertThat(matcher.evaluate(f, "D1", "S1", "Cart", "appeared", 2000, 0).type())
                .isEqualTo(FunnelDecision.Type.STEP_ADVANCED);

        FunnelDecision completed = matcher.evaluate(f, "D1", "S1", "Checkout", "appeared", 3000, 0);
        assertThat(completed.type()).isEqualTo(FunnelDecision.Type.FUNNEL_COMPLETED);
        assertThat(completed.reason()).isEqualTo("funnel_completed");
        assertThat(completed.stepsCompleted()).isEqualTo(3);

        // Tamamlanınca durum HEMEN silinir — 6dk cooldown bug'ının fix'i buydu
        assertThat(store.states).isEmpty();
        // session_once → oturum bazlı dedup anahtarı yazıldı (düzeltmeden önce YAZILMIYORDU)
        assertThat(store.visitKeys).containsExactly("funnel_visit_fired:F1:D1:S1");
    }

    @Test
    @DisplayName("çok adımlı session_once aynı oturumda ikinci kez tetiklenmez")
    void cokAdimliSessionOnceDedup() {
        Map<String, Object> f = funnel("session_once", new ArrayList<>(List.of(step("Home"), step("Cart"))));

        matcher.evaluate(f, "D1", "S1", "Home", "appeared", 1000, 0);
        assertThat(matcher.evaluate(f, "D1", "S1", "Cart", "appeared", 2000, 0).type())
                .isEqualTo(FunnelDecision.Type.FUNNEL_COMPLETED);

        // Aynı oturumda akış baştan yaşanırsa dedup devreye girer
        matcher.evaluate(f, "D1", "S1", "Home", "appeared", 3000, 0);
        FunnelDecision second = matcher.evaluate(f, "D1", "S1", "Cart", "appeared", 4000, 0);

        assertThat(second.type()).isEqualTo(FunnelDecision.Type.SKIPPED_DEDUP);
        assertThat(store.states).isEmpty();
    }

    @Test
    @DisplayName("son adımdan sonra başka event gerekmez — askıda durum kalmaz")
    void sonAdimdanSonraEventGerekmez() {
        Map<String, Object> f = funnel("session_once", new ArrayList<>(List.of(step("Home"), step("Cart"))));
        matcher.evaluate(f, "D1", "S1", "Home", "appeared", 1000, 0);

        // Düzeltmeden önce burada STEP_ADVANCED dönüyordu ve funnel askıda kalıyordu;
        // kullanıcı uygulamayı kapatırsa insight hiç gitmiyordu.
        assertThat(matcher.evaluate(f, "D1", "S1", "Cart", "appeared", 2000, 0).type())
                .isEqualTo(FunnelDecision.Type.FUNNEL_COMPLETED);
        assertThat(store.states).isEmpty();
    }

    @Test
    @DisplayName("eşleşmeyen ekran akışı ilerletmez")
    void eslesmeyenEkranIlerletmez() {
        Map<String, Object> f = funnel("session_once", new ArrayList<>(List.of(step("Home"), step("Cart"))));
        matcher.evaluate(f, "D1", "S1", "Home", "appeared", 1000, 0);

        assertThat(matcher.evaluate(f, "D1", "S1", "Profile", "appeared", 2000, 0).type())
                .isEqualTo(FunnelDecision.Type.NONE);
        assertThat(store.states.values().iterator().next().currentStep()).isEqualTo(1);
    }

    @Test
    @DisplayName("tek adımlı session_once: ilk seferde tamamlanır, ikincisinde dedup'a takılır")
    void tekAdimliSessionOnce() {
        Map<String, Object> f = funnel("session_once", new ArrayList<>(List.of(step("Home"))));

        assertThat(matcher.evaluate(f, "D1", "S1", "Home", "appeared", 1000, 0).type())
                .isEqualTo(FunnelDecision.Type.FUNNEL_COMPLETED);
        assertThat(matcher.evaluate(f, "D1", "S1", "Home", "appeared", 2000, 0).type())
                .isEqualTo(FunnelDecision.Type.SKIPPED_DEDUP);

        // Tek adımlı funnel durum tutmaz
        assertThat(store.states).isEmpty();
    }

    @Test
    @DisplayName("tek adımlı: session_once oturum bazlı, screen_visit cihaz bazlı dedup anahtarı")
    void dedupAnahtariTriggerModunaGoreDegisir() {
        Map<String, Object> sessionOnce = funnel("session_once", new ArrayList<>(List.of(step("Home"))));
        matcher.evaluate(sessionOnce, "D1", "S1", "Home", "appeared", 1000, 0);
        assertThat(store.visitKeys).containsExactly("funnel_visit_fired:F1:D1:S1");

        store.visitKeys.clear();
        Map<String, Object> screenVisit = funnel("screen_visit", new ArrayList<>(List.of(step("Home"))));
        matcher.evaluate(screenVisit, "D1", "S1", "Home", "appeared", 1000, 0);
        // sessionId YOK — aynı cihazda farklı oturumda da tetiklenmez
        assertThat(store.visitKeys).containsExactly("funnel_visit_fired:F1:D1");
    }

    @Test
    @DisplayName("global timeout: süre aşılınca timeout kararı üretilir ve durum silinir")
    void globalTimeout() {
        Map<String, Object> f = funnel("session_once", new ArrayList<>(List.of(step("Home"), step("Cart"))));
        matcher.evaluate(f, "D1", "S1", "Home", "appeared", 1000, 0);

        // globalTimeoutMs = 300_000 → 301 saniye sonra
        FunnelDecision decision = matcher.evaluate(f, "D1", "S1", "Profile", "appeared", 302_000, 0);

        assertThat(decision.type()).isEqualTo(FunnelDecision.Type.FUNNEL_TIMEOUT);
        assertThat(decision.reason()).isEqualTo("global_timeout");
        assertThat(store.states).isEmpty();
    }

    @Test
    @DisplayName("adım timeout'u MEVCUT adımın timeoutMs'ine bakar, sıradakinin değil")
    void adimTimeout() {
        // timeoutMs BİRİNCİ adımda: durum 1. adımdayken bu süre işler.
        Map<String, Object> f = funnel("session_once", new ArrayList<>(List.of(
                step("Home", "timeoutMs", 5_000), step("Cart"))));
        matcher.evaluate(f, "D1", "S1", "Home", "appeared", 1000, 0);

        // Global timeout dolmadı (300s) ama mevcut adımın timeout'u (5s) doldu
        FunnelDecision decision = matcher.evaluate(f, "D1", "S1", "Cart", "appeared", 10_000, 0);

        assertThat(decision.type()).isEqualTo(FunnelDecision.Type.FUNNEL_TIMEOUT);
        assertThat(decision.reason()).isEqualTo("step_timeout");
        assertThat(store.states).isEmpty();
    }

    @Test
    @DisplayName("minDurationMs koşulu sağlanmazsa adım ilerlemez")
    void minSureKosuluIlerlemeyiEngeller() {
        Map<String, Object> conditions = Map.of("minDurationMs", 3000);
        Map<String, Object> f = funnel("session_once", new ArrayList<>(List.of(
                step("Home"), step("Cart", "conditions", conditions))));
        matcher.evaluate(f, "D1", "S1", "Home", "appeared", 1000, 0);

        assertThat(matcher.evaluate(f, "D1", "S1", "Cart", "dwell", 2000, 2999).type())
                .isEqualTo(FunnelDecision.Type.NONE);
        // Süre koşulu sağlanınca son adım eşleşir ve funnel tamamlanır
        assertThat(matcher.evaluate(f, "D1", "S1", "Cart", "dwell", 2000, 3000).type())
                .isEqualTo(FunnelDecision.Type.FUNNEL_COMPLETED);
    }

    @Test
    @DisplayName("GCL adımı SDK matcher tarafından ilerletilmez")
    void gclAdimiBeklenir() {
        Map<String, Object> gclStep = step("Servis");
        gclStep.put("source", "gcl");
        Map<String, Object> f = funnel("session_once", new ArrayList<>(List.of(step("Home"), gclStep)));

        matcher.evaluate(f, "D1", "S1", "Home", "appeared", 1000, 0);
        assertThat(matcher.evaluate(f, "D1", "S1", "Servis", "appeared", 2000, 0).type())
                .isEqualTo(FunnelDecision.Type.NONE);
        // Durum korunur — GCL matcher ilerletecek
        assertThat(store.states.values().iterator().next().currentStep()).isEqualTo(1);
    }

    @Test
    @DisplayName("GCL ile başlayan funnel'ı SDK matcher başlatmaz")
    void gclIleBaslayanFunnelBaslatilmaz() {
        Map<String, Object> gclFirst = step("Servis");
        gclFirst.put("source", "gcl");
        Map<String, Object> f = funnel("session_once", new ArrayList<>(List.of(gclFirst, step("Home"))));

        assertThat(matcher.evaluate(f, "D1", "S1", "Servis", "appeared", 1000, 0).type())
                .isEqualTo(FunnelDecision.Type.NONE);
        assertThat(store.states).isEmpty();
    }

    @Test
    @DisplayName("bozuk durum (funnel adımları kısaltılmış) crash recovery ile tamamlanır")
    void crashRecovery() {
        Map<String, Object> f = funnel("session_once", new ArrayList<>(List.of(step("Home"), step("Cart"))));
        String key = store.stateKey("F1", "D1");
        // currentStep = 2 ama toplam 2 adım → sıradaki adım yok
        store.states.put(key, new FunnelState("F1", "D1", 2, 1000, 1000, false, null));

        FunnelDecision decision = matcher.evaluate(f, "D1", "S1", "Herhangi", "appeared", 2000, 0);

        assertThat(decision.type()).isEqualTo(FunnelDecision.Type.FUNNEL_COMPLETED);
        assertThat(decision.reason()).isEqualTo("crash_recovery");
        assertThat(store.states).isEmpty();
    }
}
