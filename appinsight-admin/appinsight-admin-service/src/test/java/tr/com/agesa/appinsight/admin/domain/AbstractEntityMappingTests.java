package tr.com.agesa.appinsight.admin.domain;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Entity ↔ şema eşleme testleri — veri kaynağından bağımsız gövde.
 *
 * <p>İki alt sınıf aynı testleri farklı şemalara karşı koşturur:
 * <ul>
 *   <li>{@link EntityMappingIT} — Testcontainers, Drizzle migration'ları sıfırdan uygulanır (CI).</li>
 *   <li>{@link LocalDbEntityMappingTest} — geliştiricinin makinesindeki canlı veritabanı.</li>
 * </ul>
 *
 * <p><b>Neden gerçek şema:</b> Hibernate'in {@code ddl-auto: create} ile ürettiği şemaya karşı
 * test etmek hiçbir şey kanıtlamaz — kendi ürettiği tabloya kendi yazar. Yakalanmak istenen
 * hata sınıfı: kolon adı sapması ({@code payload_templates.schema}), yanlış tip eşlemesi
 * ({@code insights.target_screens} jsonb mi text[] mi), jsonb round-trip bozulması.
 */
// @Transactional burada AÇIKÇA belirtilir: testler transaction içinde koşar ve sonunda
// geri alınır — canlı geliştirme veritabanına kalıcı satır yazılmaz.
@Transactional
abstract class AbstractEntityMappingTests {

    static final List<Class<?>> ALL_ENTITIES = List.of(
            AppEntity.class, ScreenEntity.class, DeviceEntity.class, SessionEntity.class,
            PayloadTemplateEntity.class, FunnelEntity.class, InsightEntity.class,
            InsightDeliveryEntity.class, DeeplinkPageEntity.class, AppMemberEntity.class,
            IntegrationEntity.class, GclQueryEntity.class, GclQueryHitEntity.class);

    @Autowired
    protected EntityManager em;

    @Test
    @DisplayName("13 entity de gerçek şemaya karşı sorgulanabiliyor")
    void tumEntitylerGercekSemayaKarsiSorgulanabilir() {
        List<String> failures = new ArrayList<>();
        for (Class<?> type : ALL_ENTITIES) {
            try {
                em.createQuery("select e from " + type.getSimpleName() + " e", type)
                        .setMaxResults(1)
                        .getResultList();
            } catch (RuntimeException ex) {
                failures.add(type.getSimpleName() + " → " + rootMessage(ex));
            }
        }

        assertThat(failures).as("şemaya karşı sorgulanamayan entity'ler").isEmpty();
        assertThat(ALL_ENTITIES).hasSize(13);
    }

    @Test
    @DisplayName("jsonb obje / dizi / string-dizisi round-trip bozulmadan dönüyor")
    void jsonbRoundTripBozulmuyor() {
        UUID appId = insertApp();

        FunnelEntity funnel = new FunnelEntity();
        funnel.setId(UUID.randomUUID());
        funnel.setAppId(appId);
        funnel.setName("Checkout Funnel");
        // steps: jsonb DİZİ (obje değil)
        funnel.setSteps(List.of(
                new LinkedHashMap<>(Map.of("order", 1, "screen", "HomeViewController", "matchType", "exact")),
                new LinkedHashMap<>(Map.of("order", 2, "screen", "CheckoutViewController", "matchType", "prefix"))));
        funnel.setTargetFilter(new LinkedHashMap<>(Map.of("platform", "ios")));
        funnel.setCreatedAt(Instant.now());
        funnel.setUpdatedAt(Instant.now());
        em.persist(funnel);

        InsightEntity insight = new InsightEntity();
        insight.setId(UUID.randomUUID());
        insight.setAppId(appId);
        insight.setTitle("Fırsat");
        // target_screens: jsonb string DİZİSİ — text[] DEĞİL
        insight.setTargetScreens(List.of("HomeViewController", "CheckoutViewController"));
        insight.setDisplay(new LinkedHashMap<>(Map.of("style", "banner", "duration_ms", 5000)));
        insight.setAction(new LinkedHashMap<>(Map.of("type", "deeplink", "page", 101)));
        insight.setFrequency(new LinkedHashMap<>(Map.of("max_per_device", 1, "window_hours", 0)));
        insight.setCreatedAt(Instant.now());
        em.persist(insight);

        em.flush();
        em.clear();

        FunnelEntity readFunnel = em.find(FunnelEntity.class, funnel.getId());
        assertThat(readFunnel.getSteps()).hasSize(2);
        assertThat(readFunnel.getSteps().get(1)).containsEntry("screen", "CheckoutViewController");
        assertThat(readFunnel.getTargetFilter()).containsEntry("platform", "ios");
        assertThat(readFunnel.getGlobalTimeoutMs()).isEqualTo(1_800_000);   // zamanlama sabiti: 30dk

        InsightEntity readInsight = em.find(InsightEntity.class, insight.getId());
        assertThat(readInsight.getTargetScreens())
                .containsExactly("HomeViewController", "CheckoutViewController");
        assertThat(readInsight.getDisplay()).containsEntry("style", "banner");
        assertThat(readInsight.getAction()).containsEntry("page", 101);
    }

    @Test
    @DisplayName("payload_templates.schema kolonu (rezerve kelime) doğru eşleniyor")
    void schemaKolonuTirnakliEslenir() {
        UUID appId = insertApp();

        PayloadTemplateEntity template = new PayloadTemplateEntity();
        template.setId(UUID.randomUUID());
        template.setAppId(appId);
        template.setName("Promo");
        template.setFieldSchema(new LinkedHashMap<>(Map.of("code", "string")));
        template.setDefaultData(new LinkedHashMap<>(Map.of("code", "SAVE10")));
        template.setPlatforms(new String[]{"ios", "android"});
        template.setCreatedAt(Instant.now());
        template.setUpdatedAt(Instant.now());
        em.persist(template);
        em.flush();
        em.clear();

        PayloadTemplateEntity read = em.find(PayloadTemplateEntity.class, template.getId());
        assertThat(read.getFieldSchema()).containsEntry("code", "string");
        assertThat(read.getPlatforms()).containsExactly("ios", "android");
    }

    @Test
    @DisplayName("sessions.id text birincil anahtar olarak çalışıyor")
    void sessionTextPrimaryKeyIleCalisir() {
        UUID appId = insertApp();
        String sdkSessionId = "SDK-" + UUID.randomUUID();

        SessionEntity session = new SessionEntity();
        session.setId(sdkSessionId);
        session.setAppId(appId);
        session.setDeviceId("7DD61454-E25A-42B0-BAF1-1740A8917159");
        session.setStartedAt(Instant.now());
        em.persist(session);
        em.flush();
        em.clear();

        assertThat(em.find(SessionEntity.class, sdkSessionId)).isNotNull();
    }

    protected UUID insertApp() {
        AppEntity app = new AppEntity();
        app.setId(UUID.randomUUID());
        app.setName("Mapping Test App");
        app.setApiKey(UUID.randomUUID().toString().replace("-", "").repeat(2));
        app.setPlatforms(new String[]{"ios"});
        app.setCreatedAt(Instant.now());
        app.setUpdatedAt(Instant.now());
        em.persist(app);
        em.flush();
        return app.getId();
    }

    private static String rootMessage(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage();
    }
}
