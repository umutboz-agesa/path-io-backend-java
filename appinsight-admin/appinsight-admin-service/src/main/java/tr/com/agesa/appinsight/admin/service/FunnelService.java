package tr.com.agesa.appinsight.admin.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.com.agesa.appinsight.admin.client.dto.FunnelDefinitionDto;
import tr.com.agesa.appinsight.admin.client.dto.FunnelDto;
import tr.com.agesa.appinsight.admin.client.dto.FunnelHistoryResponse;
import tr.com.agesa.appinsight.admin.client.dto.FunnelRequest;
import tr.com.agesa.appinsight.admin.domain.FunnelEntity;
import tr.com.agesa.appinsight.admin.repository.AppRepository;
import tr.com.agesa.appinsight.admin.repository.FunnelHistoryRepository;
import tr.com.agesa.appinsight.admin.repository.FunnelRepository;
import tr.com.agesa.appinsight.admin.repository.DeliveryHistoryRepository;
import tr.com.agesa.appinsight.common.error.AppException;
import tr.com.agesa.appinsight.common.redis.RedisKeys;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@code backend/src/api/routes/funnels.ts} + {@code services/funnelService.ts}.
 *
 * <p><b>Kapsam notu:</b> {@code POST /funnels/:id/restart} bu sınıfta YOKTUR. Restart, Redis'i
 * temizlemenin yanında bağlı SDK'lara {@code force_clear_optout} WS mesajı gönderiyor; WS ağ
 * geçidi Faz 4'te realtime mini-service'inde gelecek. Yarım uygulamak paritenin kendisini
 * bozardı (bkz. docs/BACKLOG.md).
 */
@Service
public class FunnelService {

    private final FunnelRepository funnelRepository;
    private final DeliveryHistoryRepository historyRepository;
    private final FunnelHistoryRepository summaryRepository;
    private final AppRepository appRepository;
    private final StringRedisTemplate redis;
    private final AppMapper mapper;

    public FunnelService(FunnelRepository funnelRepository,
                         DeliveryHistoryRepository historyRepository,
                         FunnelHistoryRepository summaryRepository,
                         AppRepository appRepository,
                         StringRedisTemplate redis,
                         AppMapper mapper) {
        this.funnelRepository = funnelRepository;
        this.historyRepository = historyRepository;
        this.summaryRepository = summaryRepository;
        this.appRepository = appRepository;
        this.redis = redis;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<FunnelDefinitionDto> list(UUID appId) {
        assertApp(appId);
        return funnelRepository.findByAppId(appId).stream().map(mapper::toDefinitionDto).toList();
    }

    @Transactional
    public FunnelDto create(UUID appId, FunnelRequest req) {
        assertApp(appId);

        // Node createFunnelSchema: name 1..100, steps 1..20 zorunlu.
        if (req.name() == null || req.name().isEmpty() || req.name().length() > 100) {
            throw validationFailed();
        }
        if (req.steps() == null || req.steps().isEmpty() || req.steps().size() > 20) {
            throw validationFailed();
        }
        int globalTimeoutMs = req.globalTimeoutMs() == null ? 1_800_000 : req.globalTimeoutMs();
        if (globalTimeoutMs < 60_000 || globalTimeoutMs > 86_400_000) {
            throw validationFailed();
        }

        FunnelEntity e = new FunnelEntity();
        e.setId(UUID.randomUUID());
        e.setAppId(appId);
        e.setName(req.name());
        e.setSteps(withStepDefaults(req.steps()));
        e.setGlobalTimeoutMs(globalTimeoutMs);
        e.setTargetFilter(req.targetFilter() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(req.targetFilter()));
        e.setTriggerMode(req.triggerMode() == null ? "session_once" : req.triggerMode());
        e.setActive(true);
        e.setStartsAt(parseInstant(req.startsAt()));
        e.setExpiresAt(parseInstant(req.expiresAt()));

        Instant now = Instant.now();
        e.setCreatedAt(now);
        e.setUpdatedAt(now);

        FunnelDto saved = mapper.toDto(funnelRepository.save(e));
        invalidateCache(appId);
        return saved;
    }

    /**
     * Kısmi güncelleme. Gövde ham {@code Map} olarak alınır çünkü Node her alanı
     * {@code !== undefined} ile kontrol ediyor: <b>gönderilmeyen alan korunur</b>,
     * açıkça {@code null} gönderilen alan temizlenir. Java record'u bu ikisini ayırt edemezdi
     * ({@code startsAt}/{@code expiresAt} için fark kritik).
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public FunnelDto update(UUID appId, UUID id, Map<String, Object> body) {
        assertApp(appId);
        FunnelEntity e = findFunnel(appId, id);

        if (body.containsKey("name")) {
            e.setName((String) body.get("name"));
        }
        if (body.containsKey("steps")) {
            e.setSteps(withStepDefaults((List<Map<String, Object>>) body.get("steps")));
        }
        if (body.containsKey("globalTimeoutMs")) {
            e.setGlobalTimeoutMs(((Number) body.get("globalTimeoutMs")).intValue());
        }
        if (body.containsKey("targetFilter")) {
            e.setTargetFilter(new LinkedHashMap<>((Map<String, Object>) body.get("targetFilter")));
        }
        if (body.containsKey("triggerMode")) {
            e.setTriggerMode((String) body.get("triggerMode"));
        }
        if (body.containsKey("startsAt")) {
            e.setStartsAt(parseInstant((String) body.get("startsAt")));
        }
        if (body.containsKey("expiresAt")) {
            e.setExpiresAt(parseInstant((String) body.get("expiresAt")));
        }
        e.setUpdatedAt(Instant.now());

        FunnelDto saved = mapper.toDto(funnelRepository.save(e));
        invalidateCache(appId);
        return saved;
    }

    @Transactional
    public FunnelDto toggle(UUID appId, UUID id, boolean isActive) {
        assertApp(appId);
        FunnelEntity e = findFunnel(appId, id);
        e.setActive(isActive);
        e.setUpdatedAt(Instant.now());

        FunnelDto saved = mapper.toDto(funnelRepository.save(e));
        invalidateCache(appId);
        return saved;
    }

    /** Node 404 ATMAZ — kayıt yoksa da 204 döner. Cache yine de düşürülür. */
    @Transactional
    public void delete(UUID appId, UUID id) {
        assertApp(appId);
        funnelRepository.findByIdAndAppId(id, appId).ifPresent(funnelRepository::delete);
        invalidateCache(appId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> deviceSummary(UUID appId, UUID funnelId) {
        assertApp(appId);
        return summaryRepository.deviceSummary(funnelId, appId);
    }

    @Transactional(readOnly = true)
    public FunnelHistoryResponse history(UUID appId, UUID funnelId, Integer limitParam, Integer offsetParam,
                                         String status, String userAction, String deviceId) {
        assertApp(appId);

        // Node: Math.min(Number(limit ?? 100), 200)
        int limit = Math.min(limitParam == null ? 100 : limitParam, 200);
        int offset = offsetParam == null ? 0 : offsetParam;

        DeliveryHistoryRepository.Filters filters = new DeliveryHistoryRepository.Filters(
                funnelId, appId, emptyToNull(status), emptyToNull(userAction), emptyToNull(deviceId));

        return new FunnelHistoryResponse(
                historyRepository.find(filters, limit, offset).stream().map(mapper::toDto).toList(),
                new FunnelHistoryResponse.Meta(historyRepository.count(filters), limit, offset));
    }

    // ── Yardımcılar ──────────────────────────────────────────────────────────

    /**
     * Zod step şemasındaki default'lar: {@code source: 'sdk'}, {@code matchType: 'exact'}.
     * Gövdede yoksa yazılır — funnelMatcher (Faz 3) bu alanların dolu olduğunu varsayıyor.
     */
    private static List<Map<String, Object>> withStepDefaults(List<Map<String, Object>> steps) {
        List<Map<String, Object>> result = new ArrayList<>(steps.size());
        for (Map<String, Object> step : steps) {
            Map<String, Object> copy = new LinkedHashMap<>(step);
            copy.putIfAbsent("source", "sdk");
            copy.putIfAbsent("matchType", "exact");
            result.add(copy);
        }
        return result;
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw validationFailed();
        }
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private FunnelEntity findFunnel(UUID appId, UUID id) {
        return funnelRepository.findByIdAndAppId(id, appId)
                .orElseThrow(() -> AppException.notFound("Funnel not found"));
    }

    private void assertApp(UUID appId) {
        appRepository.findByIdAndIsActiveTrue(appId)
                .orElseThrow(() -> AppException.notFound("App not found"));
    }

    /**
     * {@code funnels_cache:{appId}} anahtarını düşürür.
     *
     * <p><b>Paralel çalıştırma için kritik:</b> bu cache'i Node'un funnelMatcher'ı okuyor.
     * Java'dan funnel değiştirilip cache düşürülmezse matcher 60 saniye eski tanımla çalışır.
     */
    private void invalidateCache(UUID appId) {
        redis.delete(RedisKeys.funnelsCache(appId.toString()));
    }

    private static AppException validationFailed() {
        return new AppException("VALIDATION_ERROR", "Validation failed", 400, null);
    }
}
