package tr.com.agesa.appinsight.admin.client.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * Deeplink sayfası oluşturma/güncelleme gövdesi.
 *
 * <p>Node'da POST tam şemayı, PATCH aynı şemanın {@code .partial()} hâlini kullanır —
 * tek record ile karşılanıyor. Zorunluluk kontrolü servis katmanında yapılır
 * ({@code create} için {@code name} ve {@code pageCode} şart).
 */
public record DeeplinkPageRequest(
        @Size(min = 1, max = 80) String name,
        String description,
        @Min(1) Integer pageCode,
        @Pattern(regexp = "ios|android") String platform,
        List<Map<String, Object>> paramSchema,
        Boolean isActive
) {
}
