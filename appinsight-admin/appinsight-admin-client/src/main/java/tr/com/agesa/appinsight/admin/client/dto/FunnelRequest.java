package tr.com.agesa.appinsight.admin.client.dto;

import java.util.List;
import java.util.Map;

/**
 * Funnel oluşturma/güncelleme gövdesi. Node'da POST tam şemayı, PATCH aynı şemanın
 * {@code .partial()} hâlini kullanır; zorunluluk kontrolü servis katmanında.
 *
 * <p>{@code startsAt}/{@code expiresAt} ISO-8601 string olarak gelir ve {@code null}
 * gönderilerek temizlenebilir — bu yüzden {@code String} olarak taşınıyor, {@code Instant}
 * olarak değil (gönderilmedi ↔ null ayrımı için).
 */
public record FunnelRequest(
        String name,
        List<Map<String, Object>> steps,
        Integer globalTimeoutMs,
        Map<String, Object> targetFilter,
        String triggerMode,
        String startsAt,
        String expiresAt
) {
}
