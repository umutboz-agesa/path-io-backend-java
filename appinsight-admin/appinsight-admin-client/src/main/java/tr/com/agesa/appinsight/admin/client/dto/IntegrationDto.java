package tr.com.agesa.appinsight.admin.client.dto;

import java.time.Instant;
import java.util.Map;

/**
 * {@code integrations} tablosunun REST temsili.
 *
 * <p><b>{@code credentials} her zaman maskelenmiş döner</b> — ham servis hesabı anahtarı
 * hiçbir uçtan dışarı çıkmaz (bkz. {@code IntegrationService.maskCredentials}).
 */
public record IntegrationDto(
        String id,
        String appId,
        String type,
        Map<String, Object> config,
        Map<String, Object> credentials,
        boolean isActive,
        String status,
        String lastError,
        Instant createdAt,
        Instant updatedAt
) {
}
