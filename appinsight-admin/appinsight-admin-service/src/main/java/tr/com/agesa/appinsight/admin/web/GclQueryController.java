package tr.com.agesa.appinsight.admin.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tr.com.agesa.appinsight.admin.client.dto.GclQueryDto;
import tr.com.agesa.appinsight.admin.client.dto.GclQueryHitDto;
import tr.com.agesa.appinsight.admin.client.dto.GclQueryRequest;
import tr.com.agesa.appinsight.admin.service.GclQueryService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@code /api/v1/apps/:appId/gcl-queries} — Node'daki {@code gclQueryRoutes} karşılığı.
 *
 * <p><b>Bu kaynak diğerlerinden üç noktada ayrışıyor</b> ve üçü de bilerek korunuyor:
 * <ul>
 *   <li>Liste ve hits yanıtları <b>ham dizi</b> döner — {@code {data:[...]}} sarmalayıcısı YOK.</li>
 *   <li>Güncelleme {@code PUT} ile yapılır (projenin geri kalanı PATCH kullanıyor).</li>
 *   <li>404 gövdesi {@code {"error":"Not found"}} — {@code AppError} zarfı ({@code {error:{code,message}}}) DEĞİL.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1")
public class GclQueryController {

    private final GclQueryService gclQueryService;

    public GclQueryController(GclQueryService gclQueryService) {
        this.gclQueryService = gclQueryService;
    }

    @GetMapping("/apps/{appId}/gcl-queries")
    public List<GclQueryDto> list(@PathVariable UUID appId) {
        return gclQueryService.list(appId);
    }

    @PostMapping("/apps/{appId}/gcl-queries")
    public ResponseEntity<GclQueryDto> create(@PathVariable UUID appId,
                                              @RequestBody GclQueryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gclQueryService.create(appId, request));
    }

    @PutMapping("/apps/{appId}/gcl-queries/{id}")
    public ResponseEntity<?> update(@PathVariable UUID appId,
                                    @PathVariable UUID id,
                                    @RequestBody GclQueryRequest request) {
        return gclQueryService.update(appId, id, request)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "Not found")));
    }

    @DeleteMapping("/apps/{appId}/gcl-queries/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID appId, @PathVariable UUID id) {
        gclQueryService.delete(appId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/apps/{appId}/gcl-queries/{id}/hits")
    public List<GclQueryHitDto> hits(@PathVariable UUID appId,
                                     @PathVariable UUID id,
                                     @RequestParam(required = false) Integer limit) {
        return gclQueryService.hits(appId, id, limit);
    }

    @DeleteMapping("/apps/{appId}/gcl-queries/{id}/hits")
    public ResponseEntity<Void> deleteHits(@PathVariable UUID appId,
                                           @PathVariable UUID id,
                                           @RequestBody(required = false) DeleteHitsRequest request) {
        gclQueryService.deleteHits(appId, id, request == null ? null : request.ids());
        return ResponseEntity.noContent().build();
    }

    /** Gövde opsiyoneldir; verilmezse sorgunun TÜM hit'leri silinir. */
    public record DeleteHitsRequest(List<UUID> ids) {
    }
}
