package tr.com.agesa.appinsight.admin.client.dto;

import java.time.Instant;

/** {@code gcl_queries} — Google Cloud Logging filtre ifadeleri. */
public record GclQueryDto(
        String id,
        String appId,
        String name,
        String description,
        String expression,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
}
