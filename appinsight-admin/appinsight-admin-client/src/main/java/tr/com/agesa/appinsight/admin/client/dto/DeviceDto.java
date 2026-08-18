package tr.com.agesa.appinsight.admin.client.dto;

import java.time.Instant;
import java.util.Map;

/** {@code devices} tablosunun REST temsili (Drizzle alan adları). */
public record DeviceDto(
        String id,
        String appId,
        String deviceId,
        String platform,
        String osVersion,
        String appVersion,
        String model,
        Instant lastSeen,
        Map<String, Object> metadata
) {
}
