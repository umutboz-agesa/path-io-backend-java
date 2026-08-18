package tr.com.agesa.appinsight.admin.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Activity / session zaman serisi sorguları — TimescaleDB, native SQL.
 *
 * <h2>Neden ham {@code Map} dönüyor</h2>
 * Node bu uçlarda {@code tsdbPool.query()} sonucunu olduğu gibi JSON'a çeviriyor; yani
 * alan adları <b>snake_case</b> ({@code device_id}, {@code screen_name}) ve sıra SELECT
 * sırasıdır. Tipli DTO'ya çevirmek alan adlarını camelCase'e kaydırırdı — portal kırılırdı.
 *
 * <h2>Zaman damgası neden {@code ::text}</h2>
 * Node tarafında drizzle-orm, pg sürücüsünün timestamptz tip ayrıştırıcısını global olarak
 * değiştiriyor; {@code tsdbPool.query()} drizzle'ın eşleme katmanını atladığı için değer
 * <b>ham Postgres metni</b> olarak dönüyor: {@code 2026-04-25 20:29:42.499+00} — ISO değil,
 * {@code T}/{@code Z} yok, sondaki sıfırlar kırpılmış. Aynı çıktıyı üretmenin en güvenli yolu
 * render'ı Postgres'e bırakmak: {@code ::text}. Oturum saat dilimi UTC'ye sabitlenmiştir
 * (bkz. {@code appinsight.timescale.connection-init-sql}), aksi hâlde JDBC sürücüsü JVM'in
 * saat dilimini kullanır ve {@code +03} yazardı.
 *
 * <h2>Neden COUNT/SUM string</h2>
 * pg sürücüsü {@code int8} (bigint) değerlerini JavaScript'te <b>string</b> olarak döndürür
 * (Number güvenli tamsayı sınırı). Yani Node {@code "views":"3"} yazıyor, {@code 3} değil.
 * Sayıya çevirmek portalde tip değişikliği olurdu; string olarak taşınıyor.
 */
@Repository
public class ActivityRepository {

    private static final String ACTIVITY_COLUMNS = """
            se.time::text AS time, se.device_id, se.session_id, se.screen_name,
            se.event_type, se.duration_ms, se.platform, se.properties,
            s.model, s.os_version, s.app_version
            """;

    private final JdbcTemplate timescaleJdbcTemplate;
    private final ObjectMapper objectMapper;

    public ActivityRepository(JdbcTemplate timescaleJdbcTemplate, ObjectMapper objectMapper) {
        this.timescaleJdbcTemplate = timescaleJdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /** Belirli bir cihazın son eventleri (limit/offset uygulanır). */
    public List<Map<String, Object>> findByDevice(UUID appId, String deviceId, int limit, int offset) {
        return timescaleJdbcTemplate.query("""
                SELECT %s
                FROM screen_events se
                LEFT JOIN sessions s ON s.id = se.session_id
                WHERE se.app_id = ? AND se.device_id = ?
                ORDER BY se.time DESC
                LIMIT ? OFFSET ?
                """.formatted(ACTIVITY_COLUMNS), activityRowMapper(), appId, deviceId, limit, offset);
    }

    /**
     * Cihaz başına SON event. {@code DISTINCT ON} Postgres'e özgüdür — JPA ile ifade edilemez.
     * Node'da bu varyanta limit/offset UYGULANMAZ (meta'da dönse de sorguya girmiyor).
     */
    public List<Map<String, Object>> findLastPerDevice(UUID appId) {
        return timescaleJdbcTemplate.query("""
                SELECT DISTINCT ON (se.device_id) %s
                FROM screen_events se
                LEFT JOIN sessions s ON s.id = se.session_id
                WHERE se.app_id = ?
                ORDER BY se.device_id, se.time DESC
                """.formatted(ACTIVITY_COLUMNS), activityRowMapper(), appId);
    }

    /** Oturumun ekran bazlı özeti. {@code views} ve {@code total_duration_ms} string döner. */
    public List<Map<String, Object>> screenTimeline(String sessionId) {
        return timescaleJdbcTemplate.query("""
                SELECT
                  screen_name,
                  COUNT(*) FILTER (WHERE event_type = 'screen_view') AS views,
                  COALESCE(SUM(duration_ms), 0)                      AS total_duration_ms,
                  MIN(time)::text                                     AS first_seen,
                  MAX(time)::text                                     AS last_seen
                FROM screen_events
                WHERE session_id = ?
                GROUP BY screen_name
                ORDER BY MIN(time) ASC
                """, (rs, i) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("screen_name", rs.getString("screen_name"));
            row.put("views", rs.getString("views"));                          // bigint → string
            row.put("total_duration_ms", rs.getString("total_duration_ms"));  // bigint → string
            row.put("first_seen", rs.getString("first_seen"));
            row.put("last_seen", rs.getString("last_seen"));
            return row;
        }, sessionId);
    }

    /** Oturumun ham event listesi, zamana göre artan. */
    public List<Map<String, Object>> eventList(String sessionId) {
        return timescaleJdbcTemplate.query("""
                SELECT time::text AS time, screen_name, event_type, duration_ms, platform, properties
                FROM screen_events
                WHERE session_id = ?
                ORDER BY screen_events.time ASC
                """, (rs, i) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("time", rs.getString("time"));
            row.put("screen_name", rs.getString("screen_name"));
            row.put("event_type", rs.getString("event_type"));
            row.put("duration_ms", nullableInt(rs, "duration_ms"));
            row.put("platform", rs.getString("platform"));
            row.put("properties", readJson(rs.getString("properties")));
            return row;
        }, sessionId);
    }

    private RowMapper<Map<String, Object>> activityRowMapper() {
        return (rs, i) -> {
            // Anahtar SIRASI SELECT sırasıyla aynı olmalı — LinkedHashMap.
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("time", rs.getString("time"));
            row.put("device_id", rs.getString("device_id"));
            row.put("session_id", rs.getString("session_id"));
            row.put("screen_name", rs.getString("screen_name"));
            row.put("event_type", rs.getString("event_type"));
            row.put("duration_ms", nullableInt(rs, "duration_ms"));
            row.put("platform", rs.getString("platform"));
            row.put("properties", readJson(rs.getString("properties")));
            row.put("model", rs.getString("model"));
            row.put("os_version", rs.getString("os_version"));
            row.put("app_version", rs.getString("app_version"));
            return row;
        };
    }

    /** {@code duration_ms} NULL olabilir; {@code getInt} 0 döndüreceği için ayrıca kontrol edilir. */
    private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    /** jsonb → nesne. Node'da pg sürücüsü jsonb'yi ayrıştırıp obje döndürüyor, string değil. */
    private Object readJson(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            return raw;
        }
    }
}
