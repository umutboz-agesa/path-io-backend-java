package tr.com.agesa.appinsight.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.com.agesa.appinsight.admin.client.dto.InsightDeliveryDto;
import tr.com.agesa.appinsight.admin.client.dto.InsightDto;
import tr.com.agesa.appinsight.admin.client.dto.InsightRequest;
import tr.com.agesa.appinsight.admin.domain.InsightEntity;
import tr.com.agesa.appinsight.admin.domain.PayloadTemplateEntity;
import tr.com.agesa.appinsight.admin.repository.AppRepository;
import tr.com.agesa.appinsight.admin.repository.InsightDeliveryRepository;
import tr.com.agesa.appinsight.admin.repository.InsightRepository;
import tr.com.agesa.appinsight.admin.repository.PayloadTemplateRepository;
import tr.com.agesa.appinsight.common.error.AppException;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@code backend/src/api/routes/insights.ts} — CRUD ve teslimat listesi.
 *
 * <p><b>Kapsam notu:</b> {@code POST /insights/:id/send} ve {@code POST /apps/:appId/data-push}
 * bu sınıfta YOKTUR; ikisi de doğrudan SDK'ya WS push yapıyor ve realtime mini-service'ini
 * (Faz 4) bekliyor (bkz. docs/BACKLOG.md).
 */
@Service
public class InsightService {

    private final InsightRepository insightRepository;
    private final InsightDeliveryRepository deliveryRepository;
    private final PayloadTemplateRepository templateRepository;
    private final AppRepository appRepository;
    private final AppMapper mapper;

    public InsightService(InsightRepository insightRepository,
                          InsightDeliveryRepository deliveryRepository,
                          PayloadTemplateRepository templateRepository,
                          AppRepository appRepository,
                          AppMapper mapper) {
        this.insightRepository = insightRepository;
        this.deliveryRepository = deliveryRepository;
        this.templateRepository = templateRepository;
        this.appRepository = appRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<InsightDto> list(UUID appId) {
        assertAppExists(appId);
        return insightRepository.findByAppIdOrderByCreatedAtDesc(appId).stream().map(mapper::toDto).toList();
    }

    /** Node: bu uçta app kontrolü YOK — kayıt app_id ile birlikte aranıyor. */
    @Transactional(readOnly = true)
    public InsightDto get(UUID appId, UUID id) {
        return mapper.toDto(find(appId, id));
    }

    @Transactional
    public InsightDto create(UUID appId, InsightRequest req) {
        assertAppExists(appId);

        if (req.title() == null || req.title().isEmpty() || req.title().length() > 200) {
            throw validationFailed();
        }
        validateDisplay(req.display());
        validateAction(req.action());
        validateTarget(req.target());
        validateStatus(req.status());

        // Template verildiyse defaultData ile gövdedeki data BİRLEŞTİRİLİR; çakışmada
        // gövde kazanır ({ ...template.defaultData, ...body.data }).
        Map<String, Object> mergedData = new LinkedHashMap<>();
        UUID templateId = parseUuid(req.templateId());
        if (templateId != null) {
            PayloadTemplateEntity template = templateRepository.findByIdAndAppId(templateId, appId)
                    .orElseThrow(() -> AppException.notFound("Template not found"));
            mergedData.putAll(template.getDefaultData());
        }
        if (req.data() != null) {
            mergedData.putAll(req.data());
        }

        InsightEntity e = new InsightEntity();
        e.setId(UUID.randomUUID());
        e.setAppId(appId);
        e.setFunnelId(parseUuid(req.funnelId()));
        e.setTemplateId(templateId);
        e.setTitle(req.title());
        e.setBody(req.body() == null ? "" : req.body());
        e.setDisplay(copy(req.display()));
        e.setAction(copy(req.action()));
        e.setTarget(req.target() == null ? defaultTarget() : new LinkedHashMap<>(req.target()));
        e.setData(mergedData);
        e.setTargetScreens(req.targetScreens() == null ? new ArrayList<>() : new ArrayList<>(req.targetScreens()));
        e.setGclDataStep(req.gclDataStep());
        // Node camelCase yazıyor; kolonun DB default'u snake_case. Aynı tabloda iki biçim olabilir.
        e.setFrequency(req.frequency() == null ? defaultFrequency() : new LinkedHashMap<>(req.frequency()));
        e.setScheduledAt(parseInstant(req.scheduledAt()));
        e.setStatus(req.status() == null ? "draft" : req.status());
        e.setCreatedAt(Instant.now());

        return mapper.toDto(insightRepository.save(e));
    }

    /**
     * Kısmi güncelleme — gövde ham {@code Map} olarak alınır ({@code !== undefined} semantiği).
     *
     * <p>Gönderilmiş (sent) bir insight düzenlenemez: 400 {@code Cannot edit a sent insight}.
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public InsightDto update(UUID appId, UUID id, Map<String, Object> body) {
        // Node sırası: önce Zod doğrulaması, sonra kayıt arama, sonra 'sent' kontrolü.
        validateDisplay((Map<String, Object>) body.get("display"));
        validateAction((Map<String, Object>) body.get("action"));
        validateTarget((Map<String, Object>) body.get("target"));
        validateStatus(body.get("status"));

        InsightEntity e = find(appId, id);
        if ("sent".equals(e.getStatus())) {
            throw AppException.badRequest("Cannot edit a sent insight");
        }

        if (body.containsKey("title")) {
            e.setTitle((String) body.get("title"));
        }
        if (body.containsKey("body")) {
            e.setBody((String) body.get("body"));
        }
        if (body.containsKey("display")) {
            e.setDisplay(copy((Map<String, Object>) body.get("display")));
        }
        if (body.containsKey("action")) {
            e.setAction(copy((Map<String, Object>) body.get("action")));
        }
        if (body.containsKey("target")) {
            e.setTarget(copy((Map<String, Object>) body.get("target")));
        }
        if (body.containsKey("data")) {
            e.setData(copy((Map<String, Object>) body.get("data")));
        }
        if (body.containsKey("targetScreens")) {
            List<String> screens = (List<String>) body.get("targetScreens");
            e.setTargetScreens(screens == null ? new ArrayList<>() : new ArrayList<>(screens));
        }
        if (body.containsKey("gclDataStep")) {
            Number step = (Number) body.get("gclDataStep");
            e.setGclDataStep(step == null ? null : step.intValue());
        }
        if (body.containsKey("funnelId")) {
            e.setFunnelId(parseUuid((String) body.get("funnelId")));
        }
        if (body.containsKey("templateId")) {
            e.setTemplateId(parseUuid((String) body.get("templateId")));
        }
        if (body.containsKey("frequency")) {
            e.setFrequency(copy((Map<String, Object>) body.get("frequency")));
        }
        if (body.containsKey("status")) {
            e.setStatus((String) body.get("status"));
        }
        if (body.containsKey("scheduledAt")) {
            e.setScheduledAt(parseInstant((String) body.get("scheduledAt")));
        }
        // Node bu uçta updatedAt/sentAt'e DOKUNMUYOR (insights tablosunda updatedAt kolonu yok).

        return mapper.toDto(insightRepository.save(e));
    }

    @Transactional
    public void delete(UUID appId, UUID id) {
        insightRepository.delete(find(appId, id));
    }

    /** Node: bu uçta app veya insight varlığı kontrol EDİLMİYOR — yoksa boş liste döner. */
    @Transactional(readOnly = true)
    public List<InsightDeliveryDto> deliveries(UUID appId, UUID insightId) {
        return deliveryRepository.findByInsightIdAndAppId(insightId, appId).stream().map(mapper::toDto).toList();
    }

    // ── Doğrulama — Node'daki Zod şemalarının karşılığı ──────────────────────

    /**
     * {@code status} enum'u: draft | active | paused | archived.
     *
     * <p><b>{@code sent} bu listede YOK</b> — API'den atanamaz, yalnızca insightEngine gerçek
     * gönderim sonrası yazar. Bu yüzden "gönderilmiş insight düzenlenemez" kuralı da ancak
     * motorun yazdığı kayıtlarda tetiklenir.
     */
    private static final List<String> STATUSES = List.of("draft", "active", "paused", "archived");
    private static final List<String> DISPLAY_STYLES = List.of("banner", "modal", "toast");
    private static final List<String> ACTION_TYPES =
            List.of("deeplink", "url", "dismiss", "redirect", "return_to", "set_value");
    private static final List<String> TARGET_TYPES = List.of("all", "platform", "device", "devices");

    private static void validateStatus(Object status) {
        if (status != null && !contains(STATUSES, status)) {
            throw validationFailed();
        }
    }

    /**
     * {@code List.of(...)} immutable listtir ve {@code contains(null)} çağrısında NPE atar —
     * eksik alan 400 yerine 500 üretirdi. Bu yüzden tüm enum kontrolleri buradan geçiyor.
     */
    private static boolean contains(List<String> allowed, Object value) {
        return value instanceof String s && allowed.contains(s);
    }

    private static void validateDisplay(Map<String, Object> display) {
        if (display == null || display.isEmpty()) {
            return;
        }
        if (!contains(DISPLAY_STYLES, display.get("style"))) {
            throw validationFailed();
        }
        Object duration = display.get("duration_ms");
        if (duration != null && (!(duration instanceof Number n) || n.intValue() <= 0)) {
            throw validationFailed();
        }
    }

    private static void validateAction(Map<String, Object> action) {
        if (action == null || action.isEmpty()) {
            return;
        }
        if (!contains(ACTION_TYPES, action.get("type"))) {
            throw validationFailed();
        }
    }

    /**
     * {@code target} discriminated union: {@code all} ek alan istemez,
     * {@code platform} → {@code platform}, {@code device} → {@code device_id},
     * {@code devices} → boş olmayan {@code device_ids}.
     */
    @SuppressWarnings("unchecked")
    private static void validateTarget(Map<String, Object> target) {
        if (target == null || target.isEmpty()) {
            return;
        }
        Object type = target.get("type");
        if (!contains(TARGET_TYPES, type)) {
            throw validationFailed();
        }
        switch (String.valueOf(type)) {
            case "platform" -> {
                if (!contains(List.of("ios", "android"), target.get("platform"))) {
                    throw validationFailed();
                }
            }
            case "device" -> {
                if (!(target.get("device_id") instanceof String)) {
                    throw validationFailed();
                }
            }
            case "devices" -> {
                if (!(target.get("device_ids") instanceof List<?> ids) || ids.isEmpty()) {
                    throw validationFailed();
                }
            }
            default -> {
                // "all" — ek alan yok
            }
        }
    }

    // ── Yardımcılar ──────────────────────────────────────────────────────────

    private InsightEntity find(UUID appId, UUID id) {
        return insightRepository.findByIdAndAppId(id, appId)
                .orElseThrow(() -> AppException.notFound("Insight not found"));
    }

    private void assertAppExists(UUID appId) {
        appRepository.findByIdAndIsActiveTrue(appId)
                .orElseThrow(() -> AppException.notFound("App not found"));
    }

    private static Map<String, Object> copy(Map<String, Object> source) {
        return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
    }

    private static Map<String, Object> defaultTarget() {
        Map<String, Object> target = new LinkedHashMap<>();
        target.put("type", "all");
        return target;
    }

    private static Map<String, Object> defaultFrequency() {
        Map<String, Object> frequency = new LinkedHashMap<>();
        frequency.put("maxPerDevice", 1);
        frequency.put("windowHours", 0);
        return frequency;
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw validationFailed();
        }
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

    private static AppException validationFailed() {
        return new AppException("VALIDATION_ERROR", "Validation failed", 400, null);
    }
}
