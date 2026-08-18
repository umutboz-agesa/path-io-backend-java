package tr.com.agesa.appinsight.admin.client.dto;

import java.util.List;
import java.util.Map;

/**
 * {@code GET /api/v1/apps/:appId/sessions/:sessionId} yanıtı.
 *
 * <p>{@code session} Drizzle satırıdır (camelCase), {@code screenSummary} ve {@code eventList}
 * ham SQL sonucudur (snake_case). Aynı yanıtta iki farklı adlandırma bulunması Node'daki
 * durumdur; düzeltilmedi.
 */
public record SessionDetailResponse(
        SessionDto session,
        List<Map<String, Object>> screenSummary,
        List<Map<String, Object>> eventList
) {
}
