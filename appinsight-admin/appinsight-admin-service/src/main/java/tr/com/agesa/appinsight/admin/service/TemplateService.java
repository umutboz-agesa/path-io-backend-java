package tr.com.agesa.appinsight.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.com.agesa.appinsight.admin.client.dto.CreateTemplateRequest;
import tr.com.agesa.appinsight.admin.client.dto.TemplateDto;
import tr.com.agesa.appinsight.admin.client.dto.UpdateTemplateRequest;
import tr.com.agesa.appinsight.admin.domain.PayloadTemplateEntity;
import tr.com.agesa.appinsight.admin.repository.AppRepository;
import tr.com.agesa.appinsight.admin.repository.PayloadTemplateRepository;
import tr.com.agesa.appinsight.common.error.AppException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

/**
 * {@code backend/src/api/routes/templates.ts} karşılığı.
 *
 * <p>Node'da {@code assertAppExists} YALNIZCA create ve list uçlarında çağrılır; tekil
 * get/patch/delete uçlarında app kontrolü yoktur (kayıt app_id ile birlikte arandığı için
 * sonuç aynı, ama hata mesajı farklı olur: "App not found" yerine "Template not found").
 * Bu ayrım korunuyor.
 */
@Service
public class TemplateService {

    private final PayloadTemplateRepository templateRepository;
    private final AppRepository appRepository;
    private final AppMapper mapper;

    public TemplateService(PayloadTemplateRepository templateRepository,
                           AppRepository appRepository,
                           AppMapper mapper) {
        this.templateRepository = templateRepository;
        this.appRepository = appRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<TemplateDto> list(UUID appId) {
        assertAppExists(appId);
        return templateRepository.findByAppId(appId).stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public TemplateDto get(UUID appId, UUID id) {
        return mapper.toDto(findTemplate(appId, id));
    }

    @Transactional
    public TemplateDto create(UUID appId, CreateTemplateRequest req) {
        assertAppExists(appId);

        PayloadTemplateEntity e = new PayloadTemplateEntity();
        e.setId(UUID.randomUUID());
        e.setAppId(appId);
        e.setName(req.name());
        e.setDescription(req.description());
        e.setFieldSchema(req.fieldSchema() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(req.fieldSchema()));
        e.setDefaultData(req.defaultData() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(req.defaultData()));
        e.setPlatforms(req.platforms() == null ? new String[0] : req.platforms().toArray(String[]::new));

        Instant now = Instant.now();
        e.setCreatedAt(now);
        e.setUpdatedAt(now);

        return mapper.toDto(templateRepository.save(e));
    }

    @Transactional
    public TemplateDto update(UUID appId, UUID id, UpdateTemplateRequest req) {
        PayloadTemplateEntity e = findTemplate(appId, id);

        if (req.name() != null) {
            e.setName(req.name());
        }
        if (req.description() != null) {
            e.setDescription(req.description());
        }
        if (req.fieldSchema() != null) {
            e.setFieldSchema(new LinkedHashMap<>(req.fieldSchema()));
        }
        if (req.defaultData() != null) {
            e.setDefaultData(new LinkedHashMap<>(req.defaultData()));
        }
        if (req.platforms() != null) {
            e.setPlatforms(req.platforms().toArray(String[]::new));
        }
        e.setUpdatedAt(Instant.now());

        return mapper.toDto(templateRepository.save(e));
    }

    /** Template'te soft delete YOK — kayıt gerçekten silinir (apps'ten farklı). */
    @Transactional
    public void delete(UUID appId, UUID id) {
        templateRepository.delete(findTemplate(appId, id));
    }

    private PayloadTemplateEntity findTemplate(UUID appId, UUID id) {
        return templateRepository.findByIdAndAppId(id, appId)
                .orElseThrow(() -> AppException.notFound("Template not found"));
    }

    private void assertAppExists(UUID appId) {
        appRepository.findByIdAndIsActiveTrue(appId)
                .orElseThrow(() -> AppException.notFound("App not found"));
    }
}
