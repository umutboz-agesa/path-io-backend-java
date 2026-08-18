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
import org.springframework.web.bind.annotation.RestController;
import tr.com.agesa.appinsight.admin.client.dto.IntegrationDto;
import tr.com.agesa.appinsight.admin.client.dto.IntegrationRequest;
import tr.com.agesa.appinsight.admin.domain.IntegrationEntity;
import tr.com.agesa.appinsight.admin.service.IntegrationService;
import tr.com.agesa.appinsight.admin.service.PubSubConnectionTester;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@code /api/v1/apps/:appId/integrations} — Node'daki {@code integrationRoutes} karşılığı.
 *
 * <p>Liste yanıtı <b>ham dizi</b>dir ({@code data} sarmalayıcısı yok), güncelleme {@code PUT}.
 */
@RestController
@RequestMapping("/api/v1")
public class IntegrationController {

    /** Node: test ucunda aranan zorunlu credential alanları ve SIRASI. */
    private static final List<String> REQUIRED_CREDENTIAL_FIELDS =
            List.of("type", "project_id", "private_key", "client_email", "token_uri");

    private final IntegrationService integrationService;
    private final PubSubConnectionTester pubSubTester;

    public IntegrationController(IntegrationService integrationService, PubSubConnectionTester pubSubTester) {
        this.integrationService = integrationService;
        this.pubSubTester = pubSubTester;
    }

    @GetMapping("/apps/{appId}/integrations")
    public List<IntegrationDto> list(@PathVariable UUID appId) {
        return integrationService.list(appId);
    }

    @PostMapping("/apps/{appId}/integrations")
    public ResponseEntity<IntegrationDto> create(@PathVariable UUID appId,
                                                 @RequestBody IntegrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(integrationService.create(appId, request));
    }

    @PutMapping("/apps/{appId}/integrations/{id}")
    public IntegrationDto update(@PathVariable UUID appId,
                                 @PathVariable UUID id,
                                 @RequestBody IntegrationRequest request) {
        return integrationService.update(appId, id, request);
    }

    @DeleteMapping("/apps/{appId}/integrations/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID appId, @PathVariable UUID id) {
        integrationService.delete(appId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Pub/Sub bağlantı testi.
     *
     * <p>Hata durumunda bile <b>HTTP 200</b> döner — sorun gövdedeki {@code ok:false} ile
     * bildirilir. Her sonuçta entegrasyonun {@code status}/{@code lastError} alanları güncellenir.
     */
    @PostMapping("/apps/{appId}/integrations/{id}/test")
    public Map<String, Object> test(@PathVariable UUID appId, @PathVariable UUID id) {
        IntegrationEntity integration = integrationService.findForTest(appId, id);
        Map<String, Object> creds = integration.getCredentials();
        Map<String, Object> config = integration.getConfig();

        List<String> missing = REQUIRED_CREDENTIAL_FIELDS.stream()
                .filter(f -> isBlank(creds.get(f)))
                .toList();

        if (!missing.isEmpty()) {
            String message = "Missing credential fields: " + String.join(", ", missing);
            integrationService.markStatus(id, "error", message);
            return fail(message);
        }

        if (isBlank(config.get("projectId")) || isBlank(config.get("subscriptionName"))) {
            // DB'ye yazılan metin ile yanıttaki metin Node'da FARKLI — ikisi de korunuyor.
            integrationService.markStatus(id, "error", "projectId and subscriptionName are required");
            return fail("projectId and subscriptionName are required in config");
        }

        String error = pubSubTester.testConnection(
                String.valueOf(config.get("projectId")),
                String.valueOf(config.get("subscriptionName")),
                creds);

        if (error != null) {
            integrationService.markStatus(id, "error", error);
            return fail(error);
        }

        integrationService.markStatus(id, "connected", null);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", true);
        body.put("message", "Pub/Sub bağlantısı başarılı.");
        return body;
    }

    /** Node: {@code !creds[f]} — yani null, boş string ve false hepsi "eksik" sayılır. */
    private static boolean isBlank(Object value) {
        return value == null || "".equals(value) || Boolean.FALSE.equals(value);
    }

    private static Map<String, Object> fail(String error) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", false);
        body.put("error", error);
        return body;
    }
}
