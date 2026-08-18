package tr.com.agesa.appinsight.admin.client.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * {@code deeplink_pages} tablosunun REST temsili.
 *
 * <p>{@code pageCode} iOS'taki {@code RedirectionPageModel} ham değeridir.
 * {@code paramSchema}: {@code [{ key, type, required, label, defaultValue? }]}.
 */
public record DeeplinkPageDto(
        String id,
        String appId,
        String name,
        String description,
        int pageCode,
        String platform,
        List<Map<String, Object>> paramSchema,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
}
