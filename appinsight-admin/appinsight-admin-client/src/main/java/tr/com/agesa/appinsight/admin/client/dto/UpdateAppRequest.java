package tr.com.agesa.appinsight.admin.client.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * {@code PATCH /api/v1/apps/:id} gövdesi — tüm alanlar opsiyonel (Zod {@code .optional()}).
 *
 * <p>Node tarafında gönderilmeyen alan {@code undefined} kalır ve Drizzle {@code set()}
 * içine hiç girmez. Java'da aynı davranış için null = "dokunma" olarak yorumlanır.
 * Bu nedenle bir alanı JSON'da {@code null} göndererek NULL'a çekmek mümkün değildir —
 * Node'da da mümkün değil, davranış birebir.
 */
public record UpdateAppRequest(
        @Size(min = 1, max = 100) String name,
        List<@Pattern(regexp = "ios|android") String> platforms,
        Map<String, @Size(min = 1) String> bundleIds,
        Map<String, Object> config
) {
}
