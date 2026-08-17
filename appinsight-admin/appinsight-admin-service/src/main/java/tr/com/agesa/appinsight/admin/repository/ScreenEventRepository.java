package tr.com.agesa.appinsight.admin.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * {@code screen_events} hypertable'ı — TimescaleDB, native SQL.
 *
 * <p>JPA entity'si YOKTUR ve olmamalıdır: hypertable'da birincil anahtar yok, sorgular
 * {@code time_bucket} / {@code DISTINCT ON} gibi Postgres'e özgü ifadeler kullanıyor.
 * JPA'ya sıkıştırmak yerine ayrı havuz + native SQL (yol haritası §4).
 */
@Repository
public class ScreenEventRepository {

    private final JdbcTemplate timescaleJdbcTemplate;

    public ScreenEventRepository(JdbcTemplate timescaleJdbcTemplate) {
        this.timescaleJdbcTemplate = timescaleJdbcTemplate;
    }

    public long countByAppId(UUID appId) {
        Long count = timescaleJdbcTemplate.queryForObject(
                "select count(*) from screen_events where app_id = ?", Long.class, appId);
        return count == null ? 0L : count;
    }

    /** Hypertable gerçekten kurulu mu — bağlantı doğrulaması için. */
    public boolean hypertableExists() {
        Integer found = timescaleJdbcTemplate.queryForObject(
                "select count(*) from timescaledb_information.hypertables where hypertable_name = 'screen_events'",
                Integer.class);
        return found != null && found > 0;
    }
}
