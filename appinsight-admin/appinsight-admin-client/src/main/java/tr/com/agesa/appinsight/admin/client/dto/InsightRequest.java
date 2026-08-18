package tr.com.agesa.appinsight.admin.client.dto;

import java.util.List;
import java.util.Map;

/**
 * Insight oluşturma/güncelleme gövdesi.
 *
 * <p>{@code scheduledAt} ISO-8601 string; {@code null} gönderilerek temizlenebildiği için
 * {@code Instant} değil {@code String} olarak taşınıyor.
 */
public record InsightRequest(
        String title,
        String body,
        Map<String, Object> display,
        Map<String, Object> action,
        Map<String, Object> target,
        List<String> targetScreens,
        Integer gclDataStep,
        Map<String, Object> data,
        String templateId,
        String funnelId,
        Map<String, Object> frequency,
        String scheduledAt,
        String status
) {
}
