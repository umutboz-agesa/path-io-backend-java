package tr.com.agesa.appinsight.admin.client.dto;

import java.time.Instant;

/**
 * {@code sessions} tablosunun REST temsili.
 *
 * <p>{@code id} SDK'nın ürettiği string'dir (UUID tipi değil).
 * Oturum kapanmadıysa {@code endedAt} null döner.
 */
public record SessionDto(
        String id,
        String appId,
        String deviceId,
        String platform,
        String appVersion,
        String osVersion,
        String model,
        Instant startedAt,
        Instant endedAt
) {
}
