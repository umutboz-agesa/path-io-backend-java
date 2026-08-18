package tr.com.agesa.appinsight.admin.web;

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
import tr.com.agesa.appinsight.admin.client.dto.InsightDeliveryDto;
import tr.com.agesa.appinsight.admin.client.dto.InsightDto;
import tr.com.agesa.appinsight.admin.client.dto.InsightRequest;
import tr.com.agesa.appinsight.admin.service.InsightService;

import java.util.Map;
import java.util.UUID;

/**
 * {@code /api/v1/apps/:appId/insights} — Node'daki {@code insightsRoutes} karşılığı.
 *
 * <p><b>Eksik iki uç:</b> {@code POST /insights/:id/send} (operatörün elle push'u) ve
 * {@code POST /apps/:appId/data-push}. İkisi de bağlı SDK'lara doğrudan WS mesajı gönderiyor;
 * realtime mini-service'i (Faz 4) gelmeden yazılamaz.
 */
@RestController
@RequestMapping("/api/v1")
public class InsightController {

    private final InsightService insightService;

    public InsightController(InsightService insightService) {
        this.insightService = insightService;
    }

    @GetMapping("/apps/{appId}/insights")
    public DataResponse<InsightDto> list(@PathVariable UUID appId) {
        return DataResponse.of(insightService.list(appId));
    }

    @PostMapping("/apps/{appId}/insights")
    public ResponseEntity<InsightDto> create(@PathVariable UUID appId, @RequestBody InsightRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(insightService.create(appId, request));
    }

    @GetMapping("/apps/{appId}/insights/{id}")
    public InsightDto get(@PathVariable UUID appId, @PathVariable UUID id) {
        return insightService.get(appId, id);
    }

    @PatchMapping("/apps/{appId}/insights/{id}")
    public InsightDto update(@PathVariable UUID appId,
                             @PathVariable UUID id,
                             @RequestBody(required = false) Map<String, Object> body) {
        return insightService.update(appId, id, body == null ? Map.of() : body);
    }

    @DeleteMapping("/apps/{appId}/insights/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID appId, @PathVariable UUID id) {
        insightService.delete(appId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/apps/{appId}/insights/{id}/deliveries")
    public DataResponse<InsightDeliveryDto> deliveries(@PathVariable UUID appId, @PathVariable UUID id) {
        return DataResponse.of(insightService.deliveries(appId, id));
    }
}
