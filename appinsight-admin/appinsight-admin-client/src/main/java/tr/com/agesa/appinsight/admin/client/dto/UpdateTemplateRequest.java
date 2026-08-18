package tr.com.agesa.appinsight.admin.client.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * {@code PATCH /api/v1/apps/:appId/payload-templates/:id} gövdesi — tüm alanlar opsiyonel.
 * null = "gönderilmedi, dokunma".
 */
public record UpdateTemplateRequest(
        @Size(min = 1, max = 100) String name,
        String description,
        Map<String, Object> fieldSchema,
        Map<String, Object> defaultData,
        List<@Pattern(regexp = "ios|android") String> platforms
) {
}
