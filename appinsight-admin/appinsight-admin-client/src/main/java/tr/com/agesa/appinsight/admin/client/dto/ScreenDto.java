package tr.com.agesa.appinsight.admin.client.dto;

import java.time.Instant;
import java.util.Map;

/**
 * {@code screens} tablosunun REST temsili — {@code sdk-config} yanıtında dönülür.
 */
public record ScreenDto(
        String id,
        String appId,
        String parentId,
        String name,
        String displayName,
        String platform,
        String canonicalName,
        Instant firstSeenAt,
        Instant lastSeenAt,
        int eventCount,
        Map<String, Object> metadata,
        Instant createdAt
) {
}
