package tr.com.agesa.appinsight.admin.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * {@code GET /api/v1/apps/:id/sdk-config} yanıtı.
 *
 * <p><b>Dikkat:</b> Bu uçta alan adı {@code api_key} — snake_case. Diğer app uçları
 * camelCase döner. Tutarsız görünse de SDK bu şekli bekliyor, düzeltilmemeli.
 */
public record SdkConfigDto(
        @JsonProperty("api_key") String apiKey,
        Map<String, Object> config,
        List<ScreenDto> screens
) {
}
