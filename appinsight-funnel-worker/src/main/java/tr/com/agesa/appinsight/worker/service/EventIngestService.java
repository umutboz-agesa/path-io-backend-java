package tr.com.agesa.appinsight.worker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * {@code services/eventService.ts → process()} karşılığı: bir ekran eventini kalıcılaştırır.
 *
 * <p>Node'da dört iş yapılıyor; üçü burada:
 * <ol>
 *   <li>{@code screen_events} hypertable'ına INSERT (TimescaleDB)</li>
 *   <li>{@code screens} upsert — {@code event_count} artır, {@code last_seen_at} güncelle,
 *       kanonik ad cache'ini düşür</li>
 *   <li>{@code devices.last_seen} güncelle</li>
 *   <li><b>EKSİK:</b> portal WS'ine {@code live_event} yayını — realtime mini-service'i
 *       (Faz 4) bekliyor. Portal'ın canlı akışı bu yüzden yalnız Node worker'ı yazarken
 *       beslenir; gölge modda zaten yayın YAPILMAMALIDIR (çift kayıt olurdu).</li>
 * </ol>
 */
@Service
public class EventIngestService {

    private static final Logger log = LoggerFactory.getLogger(EventIngestService.class);

    /** Node: {@code screen_canonical:{appId}:{name}:{platform}} — funnelMatcher okuyor. */
    private static final String CANONICAL_CACHE_KEY = "screen_canonical:%s:%s:%s";

    private final JdbcTemplate timescaleJdbc;
    private final JdbcTemplate postgresJdbc;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public EventIngestService(@Qualifier("timescaleJdbcTemplate") JdbcTemplate timescaleJdbc,
                              @Qualifier("jdbcTemplate") JdbcTemplate postgresJdbc,
                              StringRedisTemplate redis,
                              ObjectMapper objectMapper) {
        this.timescaleJdbc = timescaleJdbc;
        this.postgresJdbc = postgresJdbc;
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public record ScreenEvent(
            UUID appId,
            String deviceId,
            String sessionId,
            String screenName,
            String eventType,
            Instant ts,
            Integer durationMs,
            String platform,
            Map<String, Object> properties
    ) {
    }

    /**
     * @param dryRun gölge (shadow) modda yalnızca okur, hiçbir yere yazmaz — Node ile
     *               karar karşılaştırması yapılırken çift kayıt oluşmasını engeller
     */
    public void process(ScreenEvent event, boolean dryRun) {
        if (dryRun) {
            log.debug("dry-run: {} / {} ({})", event.deviceId(), event.screenName(), event.eventType());
            return;
        }

        insertScreenEvent(event);
        upsertScreen(event);
        updateDeviceLastSeen(event);
    }

    private void insertScreenEvent(ScreenEvent e) {
        timescaleJdbc.update("""
                INSERT INTO screen_events
                  (time, app_id, device_id, session_id, screen_name, event_type, duration_ms, platform, properties)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
                """,
                Timestamp.from(e.ts()), e.appId(), e.deviceId(), e.sessionId(),
                e.screenName(), e.eventType(), e.durationMs(), e.platform(), toJson(e.properties()));
    }

    /**
     * Node {@code screenService.upsert}: unique(app_id, name, platform) çakışmasında
     * {@code last_seen_at} güncellenir ve {@code event_count} bir artar.
     *
     * <p>Ekran adı ham class adıdır — suffix temizleme YOK.
     */
    private void upsertScreen(ScreenEvent e) {
        String platform = e.platform() == null ? "" : e.platform();
        postgresJdbc.update("""
                INSERT INTO screens (id, app_id, name, display_name, platform,
                                     first_seen_at, last_seen_at, event_count, metadata, created_at)
                VALUES (gen_random_uuid(), ?, ?, ?, ?, now(), now(), 1, '{}'::jsonb, now())
                ON CONFLICT (app_id, name, platform)
                DO UPDATE SET last_seen_at = now(), event_count = screens.event_count + 1
                """, e.appId(), e.screenName(), e.screenName(), platform);

        redis.delete(CANONICAL_CACHE_KEY.formatted(e.appId(), e.screenName(), platform));
    }

    /** Node: cihaz kaydı yoksa hiçbir şey yapılmaz (INSERT değil, UPDATE). */
    private void updateDeviceLastSeen(ScreenEvent e) {
        postgresJdbc.update("UPDATE devices SET last_seen = now() WHERE app_id = ? AND device_id = ?",
                e.appId(), e.deviceId());
    }

    private String toJson(Map<String, Object> properties) {
        try {
            return objectMapper.writeValueAsString(properties == null ? Map.of() : properties);
        } catch (Exception ex) {
            return "{}";
        }
    }
}
