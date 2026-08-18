package tr.com.agesa.appinsight.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.pubsub.v1.ProjectSubscriptionName;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Pub/Sub abonelik erişimini doğrular — Node'daki
 * {@code pubsub.subscription(name).getMetadata()} çağrısının karşılığı.
 *
 * <p><b>Bilinen sapma:</b> Başarı yolu birebir aynı ({@code {"ok":true,"message":"Pub/Sub
 * bağlantısı başarılı."}}). Hata yolunda ise mesaj metni istemci kütüphanesinden geliyor;
 * Node'un {@code @google-cloud/pubsub} kütüphanesi ile Java'nın {@code google-cloud-pubsub}
 * kütüphanesi aynı durumu farklı cümlelerle anlatıyor. Yapı ({@code {"ok":false,"error":...}})
 * ve DB'ye yazılan {@code status}/{@code lastError} davranışı aynı.
 */
@Component
public class PubSubConnectionTester {

    private final ObjectMapper objectMapper;

    public PubSubConnectionTester(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Bağlantı kurulabildiyse null, kurulamadıysa hata mesajı döner. */
    public String testConnection(String projectId, String subscriptionName, Map<String, Object> credentials) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(credentials);
            GoogleCredentials googleCredentials =
                    GoogleCredentials.fromStream(new ByteArrayInputStream(json));

            SubscriptionAdminSettings settings = SubscriptionAdminSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(googleCredentials))
                    .build();

            try (SubscriptionAdminClient client = SubscriptionAdminClient.create(settings)) {
                client.getSubscription(ProjectSubscriptionName.of(projectId, subscriptionName));
            }
            return null;
        } catch (Exception e) {
            return e.getMessage() == null ? e.toString() : e.getMessage();
        }
    }
}
