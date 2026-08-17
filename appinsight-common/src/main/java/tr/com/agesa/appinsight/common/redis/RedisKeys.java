package tr.com.agesa.appinsight.common.redis;

/**
 * Redis anahtar şablonları ve TTL'leri — <b>KORUNACAK SÖZLEŞME</b>.
 *
 * <p>Node ve Java sistemleri aynı Redis üzerinde paralel çalışacağı için bu şablonların
 * bayt düzeyinde aynı kalması şarttır. Değerler mevcut TypeScript kodundan birebir alınmıştır;
 * değiştirilmesi paralel çalıştırmayı bozar (dedup kaçar, opt-out yeniden tetiklenir).
 */
public final class RedisKeys {

    private RedisKeys() {
    }

    // ── Streams ──────────────────────────────────────────────────────────────

    /** SDK ekran eventleri. Consumer group'lar: matcher-main, worker-main. */
    public static String events(String appId) {
        return "events:" + appId;
    }

    /** GCL (Google Cloud Logging) köprüsünden gelen dış eventler. Consumer group: gcl-matchers. */
    public static String extEvents(String appId) {
        return "ext_events:" + appId;
    }

    // ── Funnel durumu ────────────────────────────────────────────────────────

    /** Aktif funnel durumu. Funnel tamamlanınca HEMEN silinir (6dk cooldown bug fix). */
    public static String funnelState(String funnelId, String deviceId) {
        return "funnel_state:" + funnelId + ":" + deviceId;
    }

    /** Tetikleme dedup — session bazlı varyant. */
    public static String funnelVisitFired(String funnelId, String deviceId, String sessionId) {
        return "funnel_visit_fired:" + funnelId + ":" + deviceId + ":" + sessionId;
    }

    /** Tetikleme dedup — session'sız varyant. */
    public static String funnelVisitFired(String funnelId, String deviceId) {
        return "funnel_visit_fired:" + funnelId + ":" + deviceId;
    }

    // ── Insight teslimatı ────────────────────────────────────────────────────

    /** Frekans penceresi. TTL insight'ın frequency ayarından gelir. */
    public static String insightSent(String insightId, String deviceId) {
        return "insight_sent:" + insightId + ":" + deviceId;
    }

    /** Kalıcı opt-out — TTL yok. insight_sent'ten bağımsızdır. */
    public static String insightOptout(String insightId, String deviceId) {
        return "insight_optout:" + insightId + ":" + deviceId;
    }

    /** Offline cihaz kuyruğu. sdk_init sonrası flush edilir. */
    public static String pendingInsight(String deviceId, String insightId) {
        return "pending_insight:" + deviceId + ":" + insightId;
    }

    // ── Cache ────────────────────────────────────────────────────────────────

    public static String funnelsCache(String appId) {
        return "funnels_cache:" + appId;
    }

    // ── TTL'ler (saniye) ─────────────────────────────────────────────────────

    /** funnels_cache — funnelService.ts CACHE_TTL. */
    public static final int FUNNELS_CACHE_TTL_SEC = 60;

    /** screenService.ts CACHE_TTL (kanonik ekran adı cache'i). */
    public static final int SCREEN_CACHE_TTL_SEC = 300;

    /** pending_insight — offline cihaz retry penceresi. */
    public static final int PENDING_INSIGHT_TTL_SEC = 86_400;

    /** trigger_mode = session_once için funnel_visit_fired TTL'i. */
    public static final int VISIT_FIRED_SESSION_ONCE_TTL_SEC = 86_400;

    // ── Consumer group isimleri ──────────────────────────────────────────────
    // ASLA PID bazlı isim ekleme — zombie consumer birikimine yol açar.
    // Paralel (shadow) dönemde AYRI isim kullan, cutover'da bu isimlere geç.

    public static final String CONSUMER_GROUP_MATCHER = "matcher-main";
    public static final String CONSUMER_GROUP_EVENT_PROCESSOR = "worker-main";
    public static final String CONSUMER_GROUP_GCL = "gcl-matchers";
}
