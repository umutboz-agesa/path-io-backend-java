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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tr.com.agesa.appinsight.admin.client.dto.DataResponse;
import tr.com.agesa.appinsight.admin.client.dto.FunnelDefinitionDto;
import tr.com.agesa.appinsight.admin.client.dto.FunnelDto;
import tr.com.agesa.appinsight.admin.client.dto.FunnelHistoryResponse;
import tr.com.agesa.appinsight.admin.client.dto.FunnelRequest;
import tr.com.agesa.appinsight.admin.service.FunnelService;
import tr.com.agesa.appinsight.common.error.AppException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@code /api/v1/apps/:appId/funnels} — Node'daki {@code funnelRoutes} karşılığı.
 *
 * <p><b>Eksik uç:</b> {@code POST /funnels/:id/restart} henüz yok — Redis temizliğinin yanında
 * bağlı SDK'lara {@code force_clear_optout} WS mesajı gönderiyor ve WS ağ geçidi Faz 4'te
 * gelecek. Yarım uygulamak (Redis temizle, cihazlara haber verme) opt-out'ların cihazlarda
 * takılı kalmasına yol açardı.
 *
 * <p><b>Kontrat tuhaflığı:</b> liste yanıtı {@link FunnelDefinitionDto}, create/update yanıtı
 * {@link FunnelDto} — aynı alanlar, farklı SIRA.
 */
@RestController
@RequestMapping("/api/v1")
public class FunnelController {

    private final FunnelService funnelService;

    public FunnelController(FunnelService funnelService) {
        this.funnelService = funnelService;
    }

    @GetMapping("/apps/{appId}/funnels")
    public DataResponse<FunnelDefinitionDto> list(@PathVariable UUID appId) {
        return DataResponse.of(funnelService.list(appId));
    }

    @PostMapping("/apps/{appId}/funnels")
    public ResponseEntity<FunnelDto> create(@PathVariable UUID appId, @RequestBody FunnelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(funnelService.create(appId, request));
    }

    @PatchMapping("/apps/{appId}/funnels/{id}")
    public FunnelDto update(@PathVariable UUID appId,
                            @PathVariable UUID id,
                            @RequestBody(required = false) Map<String, Object> body) {
        return funnelService.update(appId, id, body == null ? Map.of() : body);
    }

    @PatchMapping("/apps/{appId}/funnels/{id}/toggle")
    public FunnelDto toggle(@PathVariable UUID appId,
                            @PathVariable UUID id,
                            @RequestBody(required = false) Map<String, Object> body) {
        // Node: z.object({ isActive: z.boolean() }) — alan ZORUNLU ve boolean olmalı.
        Object isActive = body == null ? null : body.get("isActive");
        if (!(isActive instanceof Boolean flag)) {
            throw new AppException("VALIDATION_ERROR", "Validation failed", 400, null);
        }
        return funnelService.toggle(appId, id, flag);
    }

    @DeleteMapping("/apps/{appId}/funnels/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID appId, @PathVariable UUID id) {
        funnelService.delete(appId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/apps/{appId}/funnels/{id}/history/devices")
    public DataResponse<Map<String, Object>> deviceSummary(@PathVariable UUID appId, @PathVariable UUID id) {
        List<Map<String, Object>> rows = funnelService.deviceSummary(appId, id);
        return DataResponse.of(rows);
    }

    @GetMapping("/apps/{appId}/funnels/{id}/history")
    public FunnelHistoryResponse history(@PathVariable UUID appId,
                                         @PathVariable UUID id,
                                         @RequestParam(required = false) Integer limit,
                                         @RequestParam(required = false) Integer offset,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(required = false) String userAction,
                                         @RequestParam(required = false) String deviceId) {
        return funnelService.history(appId, id, limit, offset, status, userAction, deviceId);
    }
}
