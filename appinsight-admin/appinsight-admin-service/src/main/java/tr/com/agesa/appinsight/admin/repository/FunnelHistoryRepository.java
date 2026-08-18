package tr.com.agesa.appinsight.admin.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Funnel geçmişinin cihaz bazlı özeti — Node'da {@code db.execute(sql\`…\`)} ile ham SQL.
 *
 * <p>Alan adları sorguda tırnaklı camelCase olarak veriliyor ({@code AS "deviceId"}), sayaçlar
 * {@code ::int} ile cast edildiği için <b>string değil sayı</b> dönüyor — {@code COUNT} normalde
 * bigint olup pg sürücüsünde string olurdu, buradaki cast bunu engelliyor.
 *
 * <p>Tarihler yine ham Postgres metni ({@code ::text}); ayrıntı için {@code ActivityRepository}.
 */
@Repository
public class FunnelHistoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public FunnelHistoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> deviceSummary(UUID funnelId, UUID appId) {
        return jdbcTemplate.query("""
                SELECT
                  device_id                                                    AS "deviceId",
                  COUNT(*)::int                                                AS total,
                  COUNT(*) FILTER (WHERE status = 'delivered')::int            AS delivered,
                  COUNT(*) FILTER (WHERE status = 'failed')::int               AS failed,
                  COUNT(*) FILTER (WHERE user_action IS NOT NULL)::int         AS "actionCount",
                  COUNT(*) FILTER (WHERE action_clicked_at IS NOT NULL)::int   AS "clickCount",
                  MAX(delivered_at)::text                                      AS "lastDeliveredAt",
                  MAX(action_clicked_at)::text                                 AS "lastActionClickedAt",
                  (
                    SELECT user_action
                    FROM insight_deliveries d2
                    WHERE d2.device_id = d.device_id
                      AND d2.funnel_id = ?
                      AND d2.app_id   = ?
                      AND d2.user_action IS NOT NULL
                    ORDER BY d2.delivered_at DESC
                    LIMIT 1
                  )                                                            AS "lastUserAction"
                FROM insight_deliveries d
                WHERE funnel_id = ?
                  AND app_id    = ?
                GROUP BY device_id
                ORDER BY MAX(delivered_at) DESC
                """, (rs, i) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("deviceId", rs.getString("deviceId"));
            row.put("total", rs.getInt("total"));
            row.put("delivered", rs.getInt("delivered"));
            row.put("failed", rs.getInt("failed"));
            row.put("actionCount", rs.getInt("actionCount"));
            row.put("clickCount", rs.getInt("clickCount"));
            row.put("lastDeliveredAt", rs.getString("lastDeliveredAt"));
            row.put("lastActionClickedAt", rs.getString("lastActionClickedAt"));
            row.put("lastUserAction", rs.getString("lastUserAction"));
            return row;
        }, funnelId, appId, funnelId, appId);
    }
}
