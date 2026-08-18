package tr.com.agesa.appinsight.worker.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tr.com.agesa.appinsight.worker.config.WorkerProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code funnel_state} ve {@code funnel_visit_fired} anahtarlarının Redis erişimi.
 *
 * <h2>Gölge (shadow) ön eki</h2>
 * Gölge modda anahtarlar {@code shadow:} ön ekiyle yazılır: {@code shadow:funnel_state:…}.
 * Böylece Java'nın durum makinesi Node'unkinin ÜSTÜNE yazmaz — iki sistem aynı Redis'te
 * birbirinden bağımsız durum tutar ve kararları karşılaştırılabilir. Cutover'da ön ek
 * boşaltılır ve Java gerçek anahtarları devralır.
 *
 * <p>Anahtar şablonlarının kendisi {@code RedisKeys}'ten gelir — sözleşme orada sabittir.
 */
@Component
public class FunnelStateStore {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final String prefix;

    public FunnelStateStore(StringRedisTemplate redis, ObjectMapper objectMapper, WorkerProperties properties) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.prefix = properties.dryRun() ? "shadow:" : "";
    }

    public String stateKey(String funnelId, String deviceId) {
        return prefix + tr.com.agesa.appinsight.common.redis.RedisKeys.funnelState(funnelId, deviceId);
    }

    public String visitKey(String funnelId, String deviceId, String sessionId) {
        String key = sessionId == null
                ? tr.com.agesa.appinsight.common.redis.RedisKeys.funnelVisitFired(funnelId, deviceId)
                : tr.com.agesa.appinsight.common.redis.RedisKeys.funnelVisitFired(funnelId, deviceId, sessionId);
        return prefix + key;
    }

    /** Node: {@code hgetall}; {@code funnel_id} yoksa durum yok sayılır. */
    public FunnelState load(String key) {
        Map<Object, Object> raw = redis.opsForHash().entries(key);
        if (raw.isEmpty() || raw.get("funnel_id") == null) {
            return null;
        }
        return new FunnelState(
                str(raw.get("funnel_id")),
                str(raw.get("device_id")),
                (int) num(raw.get("current_step")),
                num(raw.get("started_at")),
                num(raw.get("step_entered_at")),
                "1".equals(str(raw.get("completed"))),
                readJson(str(raw.get("gcl_data"))));
    }

    /** TTL: Node {@code pexpire(key, ttlMs + 60_000)} — global timeout + 1 dakika pay. */
    public void save(String key, FunnelState state, long globalTimeoutMs) {
        Map<String, String> hash = new LinkedHashMap<>();
        hash.put("funnel_id", state.funnelId());
        hash.put("device_id", state.deviceId());
        hash.put("current_step", String.valueOf(state.currentStep()));
        hash.put("started_at", String.valueOf(state.startedAt()));
        hash.put("step_entered_at", String.valueOf(state.stepEnteredAt()));
        hash.put("completed", state.completed() ? "1" : "0");
        if (state.gclData() != null && !state.gclData().isEmpty()) {
            hash.put("gcl_data", writeJson(state.gclData()));
        }

        redis.opsForHash().putAll(key, hash);
        redis.expire(key, Duration.ofMillis(globalTimeoutMs + 60_000));
    }

    public void delete(String key) {
        redis.delete(key);
    }

    public boolean exists(String key) {
        return Boolean.TRUE.equals(redis.hasKey(key));
    }

    /** Dedup anahtarını saniye TTL'i ile yazar (Node: {@code setex}). */
    public void markVisitFired(String key, long ttlSeconds) {
        redis.opsForValue().set(key, "1", Duration.ofSeconds(ttlSeconds));
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static long num(Object value) {
        try {
            return value == null ? 0L : (long) Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private Map<String, Object> readJson(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            return null;
        }
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }
}
