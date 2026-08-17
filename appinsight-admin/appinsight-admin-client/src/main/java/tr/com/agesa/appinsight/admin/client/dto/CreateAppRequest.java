package tr.com.agesa.appinsight.admin.client.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * {@code POST /api/v1/apps} gövdesi.
 *
 * <p>Doğrulama kuralları Node'daki Zod şemasıyla eşleştirilmiştir:
 * {@code name} 1..100 karakter, {@code platforms} en az 1 eleman ve yalnız
 * {@code ios|android}, {@code bundleIds} opsiyonel (verilirse alanları boş olamaz).
 */
public record CreateAppRequest(
        @NotNull @Size(min = 1, max = 100) String name,
        @NotNull @NotEmpty List<@Pattern(regexp = "ios|android") String> platforms,
        Map<String, @Size(min = 1) String> bundleIds,
        Map<String, Object> config
) {
}
