package tr.com.agesa.appinsight.admin.client.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Funnel'ın motor temsili ({@code engine.types.ts → FunnelDefinition}) — {@code GET liste}
 * yanıtında kullanılır ve Faz 3'te funnelMatcher'ın okuyacağı şekildir.
 *
 * <p>Alan sırası {@link FunnelDto}'dan farklıdır: burada {@code triggerMode},
 * {@code isActive}'ten ÖNCE gelir.
 */
public record FunnelDefinitionDto(
        String id,
        String appId,
        String name,
        List<Map<String, Object>> steps,
        int globalTimeoutMs,
        Map<String, Object> targetFilter,
        String triggerMode,
        boolean isActive,
        Instant startsAt,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt
) {
}
