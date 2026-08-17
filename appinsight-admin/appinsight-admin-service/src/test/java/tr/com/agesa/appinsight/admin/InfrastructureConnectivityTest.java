package tr.com.agesa.appinsight.admin;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import tr.com.agesa.appinsight.admin.repository.ScreenEventRepository;
import tr.com.agesa.appinsight.common.redis.RedisKeys;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Faz 0 altyapı doğrulaması: üç veri kaynağına da gerçekten bağlanılabiliyor mu?
 *
 * <p>Konfigürasyon yazmakla bağlantının çalıştığını varsaymak arasındaki farkı kapatır —
 * Redis starter'ı eklenmiş ama hiç kullanılmamışsa hata ancak Faz 4'te ortaya çıkardı.
 *
 * <p>Yerel altyapı ayakta değilse testler atlanır (kırmızı yanmaz).
 */
@SpringBootTest
@ActiveProfiles("local")
class InfrastructureConnectivityTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ScreenEventRepository screenEventRepository;

    @BeforeAll
    static void altyapiAyaktaMi() {
        assumeTrue(portAcik("localhost", 5432), "localhost:5432 kapalı — altyapı testi atlandı");
        assumeTrue(portAcik("localhost", 6379), "localhost:6379 kapalı — altyapı testi atlandı");
    }

    @Test
    @DisplayName("Redis'e yazılıp okunabiliyor ve anahtar formatı korunuyor")
    void redisBaglantisiCalisiyor() {
        String deviceId = "TEST-" + UUID.randomUUID();
        String key = RedisKeys.pendingInsight(deviceId, "insight-1");

        // Anahtar şablonu sözleşmenin parçası — biçimi de doğrula.
        assertThat(key).isEqualTo("pending_insight:" + deviceId + ":insight-1");

        try {
            redisTemplate.opsForValue().set(key, "insight-1");
            assertThat(redisTemplate.opsForValue().get(key)).isEqualTo("insight-1");
        } finally {
            redisTemplate.delete(key);   // canlı Redis'e kalıcı anahtar bırakma
        }
    }

    @Test
    @DisplayName("TimescaleDB ayrı havuzdan sorgulanabiliyor ve hypertable kurulu")
    void timescaleBaglantisiCalisiyor() {
        assertThat(screenEventRepository.hypertableExists())
                .as("screen_events hypertable olarak kurulu olmalı")
                .isTrue();

        // Bilinmeyen app için 0 dönmeli — sorgu çalışıyor demektir.
        assertThat(screenEventRepository.countByAppId(UUID.randomUUID())).isZero();
    }

    private static boolean portAcik(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
