package tr.com.agesa.appinsight.admin.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tr.com.agesa.appinsight.admin.client.dto.DataResponse;
import tr.com.agesa.appinsight.admin.client.dto.DeeplinkPageDto;
import tr.com.agesa.appinsight.admin.client.dto.DeeplinkPageRequest;
import tr.com.agesa.appinsight.admin.service.DeeplinkPageService;

import java.util.UUID;

/**
 * {@code /api/v1/apps/:appId/deeplink-pages} — Node'daki {@code deeplinkPageRoutes} karşılığı.
 *
 * <p>Kontrat notu: DELETE, kayıt bulunamasa bile 404 ATMAZ, 204 döner (Node'da da öyle).
 */
@RestController
@RequestMapping("/api/v1")
public class DeeplinkPageController {

    private final DeeplinkPageService deeplinkPageService;

    public DeeplinkPageController(DeeplinkPageService deeplinkPageService) {
        this.deeplinkPageService = deeplinkPageService;
    }

    @GetMapping("/apps/{appId}/deeplink-pages")
    public DataResponse<DeeplinkPageDto> list(@PathVariable UUID appId) {
        return DataResponse.of(deeplinkPageService.list(appId));
    }

    @PostMapping("/apps/{appId}/deeplink-pages")
    public ResponseEntity<DeeplinkPageDto> create(@PathVariable UUID appId,
                                                  @Valid @RequestBody DeeplinkPageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(deeplinkPageService.create(appId, request));
    }

    @PatchMapping("/apps/{appId}/deeplink-pages/{id}")
    public DeeplinkPageDto update(@PathVariable UUID appId,
                                  @PathVariable UUID id,
                                  @Valid @RequestBody DeeplinkPageRequest request) {
        return deeplinkPageService.update(appId, id, request);
    }

    @DeleteMapping("/apps/{appId}/deeplink-pages/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID appId, @PathVariable UUID id) {
        deeplinkPageService.delete(appId, id);
        return ResponseEntity.noContent().build();
    }
}
