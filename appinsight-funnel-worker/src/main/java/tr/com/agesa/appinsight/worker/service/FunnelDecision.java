package tr.com.agesa.appinsight.worker.service;

/**
 * Matcher'ın bir event için verdiği karar.
 *
 * <h2>Neden ayrı bir tip</h2>
 * Node'da funnelMatcher kararı doğrudan {@code insightEngine.evaluate()} çağırarak uyguluyor.
 * insightEngine Faz 4'te (realtime mini-service) gelecek; o gelene kadar matcher kararını
 * <b>üretir ama uygulamaz</b>. Bu, gölge modda Node ile <b>karar karşılaştırması</b> yapmayı
 * mümkün kılıyor: aynı event akışında iki sistemin ürettiği karar dizisi birebir aynı olmalı.
 *
 * <p>Faz 4'te {@link Type#FUNNEL_COMPLETED} ve {@link Type#FUNNEL_TIMEOUT} kararları
 * insightEngine'e bağlanacak; diğerleri iç durum geçişleri olarak kalacak.
 */
public record FunnelDecision(
        Type type,
        String funnelId,
        String deviceId,
        String screen,
        int stepsCompleted,
        int totalSteps,
        long ts,
        String reason
) {

    public enum Type {
        /** Hiçbir şey olmadı — event bu funnel'ı ilgilendirmiyor. */
        NONE,
        /** Çok adımlı funnel'ın ilk adımı eşleşti, durum oluşturuldu. */
        FUNNEL_STARTED,
        /** Ara adım ilerledi. */
        STEP_ADVANCED,
        /** Funnel tamamlandı — insightEngine'e gidecek karar (Faz 4). */
        FUNNEL_COMPLETED,
        /** Global veya adım timeout'u doldu — insightEngine'e gidecek karar (Faz 4). */
        FUNNEL_TIMEOUT,
        /** Tamamlanacaktı ama dedup anahtarı (funnel_visit_fired) engelledi. */
        SKIPPED_DEDUP
    }

    public static FunnelDecision none() {
        return new FunnelDecision(Type.NONE, null, null, null, 0, 0, 0, null);
    }

    /** Karar diff'i için tek satırlık, karşılaştırılabilir gösterim. */
    public String toDiffLine() {
        return "%s funnel=%s device=%s screen=%s step=%d/%d".formatted(
                type, funnelId, deviceId, screen, stepsCompleted, totalSteps);
    }
}
