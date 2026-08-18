package tr.com.agesa.appinsight.admin.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import tr.com.agesa.appinsight.admin.client.dto.ScreenListResponse;
import tr.com.agesa.appinsight.admin.domain.ScreenEntity;
import tr.com.agesa.appinsight.admin.repository.ScreenRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Ekran gruplama davranışı — özellikle {@code lastSeenAt} seçimi.
 *
 * <p>Canlı veride çok üyeli grup bulunmadığı için parite karşılaştırması bu davranışı
 * sınamıyor; test burada üstleniyor.
 */
class ScreenServiceGroupingTest {

    private final ScreenRepository repository = mock(ScreenRepository.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ScreenService service = new ScreenService(repository, redis, new AppMapper());

    /**
     * <b>Node'daki hatanın birebir kopyalandığını doğrular.</b>
     *
     * <p>{@code [Date, Date].sort().at(-1)} JavaScript'te tarihleri string'e çevirip sıralar:
     * "Mon Aug 17 …" &lt; "Sat Apr 25 …" olduğu için <i>daha eski</i> olan Cumartesi seçilir.
     * Doğru davranış Ağustos tarihini seçmek olurdu.
     *
     * <p>Bu test kırmızı yanarsa iki şeyden biri olmuştur: ya replikasyon bozulmuştur
     * (parite kaybı), ya da hata bilerek düzeltilmiştir — o durumda Node tarafı da
     * düzeltilmeli ve bu test güncellenmelidir (bkz. docs/BACKLOG.md).
     */
    @Test
    @DisplayName("grup lastSeenAt'i Node'un hatalı sıralamasıyla aynı sonucu veriyor")
    void grupLastSeenAtNodeIleAyniSeciliyor() {
        UUID appId = UUID.randomUUID();
        Instant cumartesiNisan = Instant.parse("2026-04-25T20:29:45Z");   // "Sat Apr 25 2026"
        Instant pazartesiAgustos = Instant.parse("2026-08-17T14:30:09Z"); // "Mon Aug 17 2026" — DAHA YENİ

        given(repository.findByAppIdOrderByCanonicalNameAscNameAsc(appId))
                .willReturn(List.of(
                        screen(appId, "HomeViewController", "ios", cumartesiNisan, 10),
                        screen(appId, "HomeActivity", "android", pazartesiAgustos, 5)));

        ScreenListResponse response = service.list(appId);

        assertThat(response.groups()).hasSize(1);
        ScreenListResponse.Group group = response.groups().get(0);

        assertThat(group.lastSeenAt())
                .as("Node 'Sat' > 'Mon' alfabetik sıralamasıyla ESKİ tarihi seçiyor")
                .isEqualTo(cumartesiNisan)
                .isNotEqualTo(pazartesiAgustos);

        // Gruplamanın geri kalanı doğru çalışıyor
        assertThat(group.canonicalName()).isEqualTo("Ana Sayfa");
        assertThat(group.platforms()).containsExactly("ios", "android");
        assertThat(group.totalEvents()).isEqualTo(15);
        assertThat(group.members()).hasSize(2);
    }

    @Test
    @DisplayName("canonicalName yoksa displayName ile gruplanıyor, boş platform elenir")
    void canonicalNameYoksaDisplayNameIleGruplanir() {
        UUID appId = UUID.randomUUID();
        ScreenEntity noCanonical = screen(appId, "SettingsViewController", "", Instant.now(), 3);
        noCanonical.setCanonicalName(null);

        given(repository.findByAppIdOrderByCanonicalNameAscNameAsc(appId)).willReturn(List.of(noCanonical));

        ScreenListResponse.Group group = service.list(appId).groups().get(0);

        assertThat(group.canonicalName()).isEqualTo("SettingsViewController");
        assertThat(group.platforms()).as("boş platform listeye girmemeli").isEmpty();
    }

    private static ScreenEntity screen(UUID appId, String name, String platform, Instant lastSeen, int events) {
        ScreenEntity e = new ScreenEntity();
        e.setId(UUID.randomUUID());
        e.setAppId(appId);
        e.setName(name);
        e.setDisplayName(name);
        e.setPlatform(platform);
        e.setCanonicalName("Ana Sayfa");
        e.setFirstSeenAt(lastSeen);
        e.setLastSeenAt(lastSeen);
        e.setEventCount(events);
        e.setCreatedAt(lastSeen);
        return e;
    }
}
