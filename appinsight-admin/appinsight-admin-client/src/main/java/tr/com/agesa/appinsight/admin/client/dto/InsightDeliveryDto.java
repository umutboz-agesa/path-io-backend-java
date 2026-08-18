package tr.com.agesa.appinsight.admin.client.dto;

import java.time.Instant;
import java.util.Map;

/** {@code insight_deliveries} — teslimat ve kullanıcı aksiyonu geçmişi. */
public record InsightDeliveryDto(
        String id,
        String insightId,
        String appId,
        String deviceId,
        String status,
        String funnelId,
        Map<String, Object> triggerCtx,
        Instant deliveredAt,
        String userAction,
        Instant interactedAt,
        Instant actionClickedAt
) {
}
