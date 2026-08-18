package tr.com.agesa.appinsight.admin.client.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Funnel'ın <b>ham satır</b> temsili — {@code POST} ve {@code PATCH} yanıtlarında kullanılır.
 *
 * <p><b>Dikkat:</b> {@code GET liste} bunu DEĞİL {@link FunnelDefinitionDto}'yu döner ve
 * ikisinde alan SIRASI farklıdır ({@code isActive} ↔ {@code triggerMode}). Node'da liste
 * {@code toDefinition()}'dan, create/update ise doğrudan Drizzle satırından geçtiği için
 * oluşan bir tutarsızlık; korunuyor.
 */
public record FunnelDto(
        String id,
        String appId,
        String name,
        List<Map<String, Object>> steps,
        int globalTimeoutMs,
        Map<String, Object> targetFilter,
        boolean isActive,
        String triggerMode,
        Instant startsAt,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt
) {
}
