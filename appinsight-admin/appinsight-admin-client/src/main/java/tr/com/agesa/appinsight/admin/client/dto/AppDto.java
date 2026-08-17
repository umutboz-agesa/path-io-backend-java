package tr.com.agesa.appinsight.admin.client.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * {@code apps} tablosunun REST temsili.
 *
 * <p>Alan adları ve sırası Node'un Drizzle {@code select()} çıktısıyla birebir aynıdır
 * (camelCase). Web portal bu şekle bağlı — değiştirilmemeli.
 */
public record AppDto(
        String id,
        String name,
        String apiKey,
        Map<String, Object> bundleIds,
        List<String> platforms,
        Map<String, Object> config,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
}
