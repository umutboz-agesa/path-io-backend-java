package tr.com.agesa.appinsight.admin.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tr.com.agesa.appinsight.admin.client.dto.AppDto;
import tr.com.agesa.appinsight.admin.client.dto.CreateAppRequest;
import tr.com.agesa.appinsight.admin.client.dto.PagedResponse;
import tr.com.agesa.appinsight.admin.client.dto.SdkConfigDto;
import tr.com.agesa.appinsight.admin.client.dto.UpdateAppRequest;
import tr.com.agesa.appinsight.admin.service.AppService;

import java.util.UUID;

/**
 * {@code /api/v1/apps} — Node'daki {@code appsRoutes} ile birebir aynı yol, gövde ve status kodları.
 *
 * <p>Kontrat notları:
 * <ul>
 *   <li>Liste yanıtı sarmalanır ({@code data}/{@code meta}), tekil yanıtlar sarmalanmaz.</li>
 *   <li>POST → 201, DELETE → 204 (gövdesiz, soft delete).</li>
 *   <li>Bulunamayan kayıt → 404 {@code {"error":{"code":"NOT_FOUND","message":"App not found"}}}.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1")
@Validated
public class AppController {

    private final AppService appService;

    public AppController(AppService appService) {
        this.appService = appService;
    }

    @GetMapping("/apps")
    public PagedResponse<AppDto> list(
            @RequestParam(defaultValue = "1") @Positive int page,
            @RequestParam(defaultValue = "20") @Positive @Max(100) int limit,
            @RequestParam(required = false) String search) {
        return appService.list(page, limit, search);
    }

    @PostMapping("/apps")
    public ResponseEntity<AppDto> create(@Valid @RequestBody CreateAppRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appService.create(request));
    }

    @GetMapping("/apps/{id}")
    public AppDto get(@PathVariable UUID id) {
        return appService.get(id);
    }

    @PatchMapping("/apps/{id}")
    public AppDto update(@PathVariable UUID id, @Valid @RequestBody UpdateAppRequest request) {
        return appService.update(id, request);
    }

    @DeleteMapping("/apps/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        appService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/apps/{id}/sdk-config")
    public SdkConfigDto sdkConfig(@PathVariable UUID id) {
        return appService.sdkConfig(id);
    }
}
