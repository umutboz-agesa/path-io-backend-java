package tr.com.agesa.appinsight.admin.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tr.com.agesa.appinsight.admin.client.dto.ScreenListResponse;
import tr.com.agesa.appinsight.admin.service.ScreenService;
import tr.com.agesa.appinsight.common.error.AppException;

import java.util.Map;
import java.util.UUID;

/**
 * {@code /api/v1/apps/:appId/screens} — Node'daki {@code screensRoutes} karşılığı.
 *
 * <p>Kontrat notu: PATCH ve DELETE, kayıt bulunamasa bile 404 ATMAZ (Node'da da atmıyor).
 * PATCH gövdesiz 200 {@code {"ok":true}} döner.
 */
@RestController
@RequestMapping("/api/v1")
public class ScreenController {

    private final ScreenService screenService;

    public ScreenController(ScreenService screenService) {
        this.screenService = screenService;
    }

    @GetMapping("/apps/{appId}/screens")
    public ScreenListResponse list(@PathVariable UUID appId) {
        return screenService.list(appId);
    }

    @PatchMapping("/apps/{appId}/screens/{id}")
    public Map<String, Boolean> update(@PathVariable UUID appId,
                                       @PathVariable UUID id,
                                       @RequestBody(required = false) Map<String, Object> body) {
        screenService.updateCanonical(appId, id, canonicalNameOf(body));
        return Map.of("ok", true);
    }

    /**
     * Node'daki Zod şeması: {@code z.string().min(1).max(100).nullable()} — <b>nullable ama
     * ZORUNLU</b>. Yani alan gövdede bulunmak zorunda, değeri {@code null} olabilir
     * (gruplamayı kaldırmak için).
     *
     * <p>Java record'u bu ayrımı yapamaz: gönderilmeyen alan da {@code null} gelir, açıkça
     * {@code null} gönderilen de. Bu yüzden gövde ham {@code Map} olarak alınıp anahtarın
     * VARLIĞI kontrol ediliyor — {@code @Valid} ile bu davranış üretilemezdi.
     */
    private static String canonicalNameOf(Map<String, Object> body) {
        if (body == null || !body.containsKey("canonicalName")) {
            throw new AppException("VALIDATION_ERROR", "Validation failed", 400, null);
        }
        Object value = body.get("canonicalName");
        if (value == null) {
            return null;                       // gruplamayı kaldır — geçerli
        }
        if (!(value instanceof String s) || s.isEmpty() || s.length() > 100) {
            throw new AppException("VALIDATION_ERROR", "Validation failed", 400, null);
        }
        return s;
    }

    @DeleteMapping("/apps/{appId}/screens/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID appId, @PathVariable UUID id) {
        screenService.delete(appId, id);
        return ResponseEntity.noContent().build();
    }
}
