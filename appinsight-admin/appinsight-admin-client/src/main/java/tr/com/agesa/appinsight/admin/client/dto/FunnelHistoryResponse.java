package tr.com.agesa.appinsight.admin.client.dto;

import java.util.List;

/** {@code GET /apps/:appId/funnels/:id/history} yanıtı. */
public record FunnelHistoryResponse(List<InsightDeliveryDto> data, Meta meta) {

    /** Alan sırası Node ile aynı: total, limit, offset. */
    public record Meta(long total, int limit, int offset) {
    }
}
