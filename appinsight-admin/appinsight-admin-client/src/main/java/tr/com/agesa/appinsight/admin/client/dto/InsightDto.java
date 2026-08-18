package tr.com.agesa.appinsight.admin.client.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * {@code insights} tablosunun REST temsili — alan sırası Drizzle kolon sırasıyla aynı.
 *
 * <p>{@code frequency} kayıt oluşturulurken Node camelCase yazıyor
 * ({@code {maxPerDevice, windowHours}}) ama kolonun DB default'u snake_case
 * ({@code {max_per_device, window_hours}}). Yani aynı tabloda iki farklı biçim bulunabilir;
 * insightEngine (Faz 4) ikisini de okumak zorunda.
 */
public record InsightDto(
        String id,
        String appId,
        String funnelId,
        String templateId,
        String title,
        String body,
        Map<String, Object> display,
        Map<String, Object> action,
        Map<String, Object> target,
        Map<String, Object> data,
        List<String> targetScreens,
        Integer gclDataStep,
        String status,
        Map<String, Object> frequency,
        Instant scheduledAt,
        Instant createdAt,
        Instant sentAt
) {
}
