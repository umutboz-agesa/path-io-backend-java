package tr.com.agesa.appinsight.admin.client.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * {@code payload_templates} tablosunun REST temsili.
 *
 * <p>Alan adı {@code fieldSchema}, kolon adı {@code schema} — JSON'da Drizzle alan adı
 * kullanılır, yani {@code fieldSchema}.
 */
public record TemplateDto(
        String id,
        String appId,
        String name,
        String description,
        Map<String, Object> fieldSchema,
        Map<String, Object> defaultData,
        List<String> platforms,
        Instant createdAt,
        Instant updatedAt
) {
}
