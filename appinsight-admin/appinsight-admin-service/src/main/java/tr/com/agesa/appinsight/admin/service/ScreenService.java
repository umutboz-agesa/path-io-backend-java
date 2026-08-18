package tr.com.agesa.appinsight.admin.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.com.agesa.appinsight.admin.client.dto.ScreenDto;
import tr.com.agesa.appinsight.admin.client.dto.ScreenListResponse;
import tr.com.agesa.appinsight.admin.domain.ScreenEntity;
import tr.com.agesa.appinsight.admin.repository.ScreenRepository;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * {@code backend/src/api/routes/screens.ts} + {@code services/screenService.ts} karşılığı.
 */
@Service
public class ScreenService {

    /** Node: {@code screen_canonical:{appId}:{name}:{platform}} — funnelMatcher bu cache'i okuyor. */
    private static final String CANONICAL_CACHE_KEY = "screen_canonical:%s:%s:%s";

    private final ScreenRepository screenRepository;
    private final StringRedisTemplate redis;
    private final AppMapper mapper;

    public ScreenService(ScreenRepository screenRepository, StringRedisTemplate redis, AppMapper mapper) {
        this.screenRepository = screenRepository;
        this.redis = redis;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public ScreenListResponse list(UUID appId) {
        List<ScreenEntity> rows = screenRepository.findByAppIdOrderByCanonicalNameAscNameAsc(appId);

        // Gruplama anahtarı: canonicalName varsa o, yoksa displayName.
        // LinkedHashMap — grupların çıkış sırası ilk görüldükleri sıradır (Node'da da öyle).
        Map<String, List<ScreenEntity>> groups = new LinkedHashMap<>();
        for (ScreenEntity row : rows) {
            String key = row.getCanonicalName() != null ? row.getCanonicalName() : row.getDisplayName();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
        }

        List<ScreenListResponse.Group> groupDtos = new ArrayList<>();
        for (Map.Entry<String, List<ScreenEntity>> entry : groups.entrySet()) {
            List<ScreenEntity> members = entry.getValue();
            groupDtos.add(new ScreenListResponse.Group(
                    entry.getKey(),
                    members.stream()
                            .map(ScreenEntity::getPlatform)
                            .filter(p -> p != null && !p.isEmpty())   // Node: .filter(Boolean)
                            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
                            .stream().toList(),
                    members.stream().mapToLong(ScreenEntity::getEventCount).sum(),
                    lastSeenAtNodeUyumlu(members),
                    members.stream().map(mapper::toDto).toList()));
        }

        return new ScreenListResponse(rows.stream().map(mapper::toDto).toList(), groupDtos);
    }

    /**
     * Node'daki {@code members.map(m => m.lastSeenAt).sort().at(-1)} ifadesinin BİREBİR karşılığı.
     *
     * <p><b>Bu bir hata — bilerek kopyalanıyor.</b> JavaScript'te {@code Array.prototype.sort()}
     * karşılaştırıcı verilmezse elemanları <i>string'e çevirip</i> sıralar. {@code Date} nesnesi
     * {@code "Mon Aug 17 2026 17:30:09 GMT+0300 (...)"} biçimine dönüşür ve sıralama haftanın
     * gün adına göre <i>alfabetik</i> yapılır: "Fri" &lt; "Mon" &lt; "Sat" &lt; "Sun" &lt; "Thu"...
     * Yani en yeni tarih değil, gün adı alfabetik olarak en sondaki seçilir.
     *
     * <p>Doğrusu {@code max()} olurdu. Ama bu proje davranışsal parite üzerine kurulu: burada
     * "düzeltmek", gölge trafik karşılaştırmasında sürekli fark üretir ve gerçek regresyonları
     * gizler. Bu yüzden hata taşınıyor, {@code docs/BACKLOG.md}'ye kaydedildi ve cutover sonrası
     * İKİ sistemde birlikte düzeltilecek.
     */
    private static Instant lastSeenAtNodeUyumlu(List<ScreenEntity> members) {
        return members.stream()
                .map(ScreenEntity::getLastSeenAt)
                .max(Comparator.comparing(ScreenService::jsDateToString))
                .orElse(null);
    }

    /** JavaScript {@code Date.prototype.toString()} çıktısını üretir. */
    private static String jsDateToString(Instant instant) {
        return DateTimeFormatter
                .ofPattern("EEE MMM dd yyyy HH:mm:ss 'GMT'Z", Locale.ENGLISH)
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }

    /**
     * Kanonik ad ata/kaldır. Node 404 ATMAZ — kayıt yoksa da {@code {ok:true}} döner.
     */
    @Transactional
    public void updateCanonical(UUID appId, UUID id, String canonicalName) {
        screenRepository.findByIdAndAppId(id, appId).ifPresent(screen -> {
            screen.setCanonicalName(canonicalName);
            screenRepository.save(screen);
            invalidateCanonicalCache(appId, screen);
        });
    }

    /** Node 404 ATMAZ — kayıt yoksa da 204 döner. */
    @Transactional
    public void delete(UUID appId, UUID id) {
        screenRepository.findByIdAndAppId(id, appId).ifPresent(screen -> {
            screenRepository.delete(screen);
            invalidateCanonicalCache(appId, screen);
        });
    }

    /**
     * Kanonik ad cache'ini düşür.
     *
     * <p><b>Paralel çalıştırma için kritik:</b> bu cache'i Node'un funnelMatcher'ı okuyor.
     * Java tarafında ekran adı değişip cache düşürülmezse funnelMatcher 5 dakika boyunca
     * eski kanonik adla eşleştirme yapar.
     */
    private void invalidateCanonicalCache(UUID appId, ScreenEntity screen) {
        redis.delete(CANONICAL_CACHE_KEY.formatted(appId, screen.getName(), screen.getPlatform()));
    }

    /** {@code sdk-config} ucu için — app'e ait tüm ekranlar. */
    @Transactional(readOnly = true)
    public List<ScreenDto> findByAppId(UUID appId) {
        return screenRepository.findByAppId(appId).stream().map(mapper::toDto).toList();
    }
}
