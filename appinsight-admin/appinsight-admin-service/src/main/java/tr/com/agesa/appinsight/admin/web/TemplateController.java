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
import tr.com.agesa.appinsight.admin.client.dto.CreateTemplateRequest;
import tr.com.agesa.appinsight.admin.client.dto.DataResponse;
import tr.com.agesa.appinsight.admin.client.dto.TemplateDto;
import tr.com.agesa.appinsight.admin.client.dto.UpdateTemplateRequest;
import tr.com.agesa.appinsight.admin.service.TemplateService;

import java.util.UUID;

/**
 * {@code /api/v1/apps/:appId/payload-templates} — Node'daki {@code templatesRoutes} karşılığı.
 *
 * <p>Liste yanıtı {@code {data:[...]}} — apps'teki gibi {@code meta} YOKTUR.
 */
@RestController
@RequestMapping("/api/v1")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping("/apps/{appId}/payload-templates")
    public DataResponse<TemplateDto> list(@PathVariable UUID appId) {
        return DataResponse.of(templateService.list(appId));
    }

    @PostMapping("/apps/{appId}/payload-templates")
    public ResponseEntity<TemplateDto> create(@PathVariable UUID appId,
                                              @Valid @RequestBody CreateTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.create(appId, request));
    }

    @GetMapping("/apps/{appId}/payload-templates/{id}")
    public TemplateDto get(@PathVariable UUID appId, @PathVariable UUID id) {
        return templateService.get(appId, id);
    }

    @PatchMapping("/apps/{appId}/payload-templates/{id}")
    public TemplateDto update(@PathVariable UUID appId,
                              @PathVariable UUID id,
                              @Valid @RequestBody UpdateTemplateRequest request) {
        return templateService.update(appId, id, request);
    }

    @DeleteMapping("/apps/{appId}/payload-templates/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID appId, @PathVariable UUID id) {
        templateService.delete(appId, id);
        return ResponseEntity.noContent().build();
    }
}
