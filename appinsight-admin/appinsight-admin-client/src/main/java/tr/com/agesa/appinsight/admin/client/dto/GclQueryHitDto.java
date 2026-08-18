package tr.com.agesa.appinsight.admin.client.dto;

import java.time.Instant;
import java.util.Map;

/**
 * {@code gcl_query_hits} — bir GCL sorgusunun eşleştiği olay.
 *
 * <p>{@code ts} olayın GCL'deki zamanı, {@code createdAt} kaydın yazılma zamanı.
 */
public record GclQueryHitDto(
        String id,
        String queryId,
        String appId,
        String deviceId,
        Instant ts,
        Map<String, Object> eventData,
        Instant createdAt
) {
}
