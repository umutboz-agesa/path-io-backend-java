package tr.com.agesa.appinsight.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.com.agesa.appinsight.admin.client.dto.DeeplinkPageDto;
import tr.com.agesa.appinsight.admin.client.dto.DeeplinkPageRequest;
import tr.com.agesa.appinsight.admin.domain.DeeplinkPageEntity;
import tr.com.agesa.appinsight.admin.repository.DeeplinkPageRepository;
import tr.com.agesa.appinsight.common.error.AppException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * {@code backend/src/api/routes/deeplinkPages.ts} karşılığı.
 */
@Service
public class DeeplinkPageService {

    private final DeeplinkPageRepository repository;
    private final AppMapper mapper;

    public DeeplinkPageService(DeeplinkPageRepository repository, AppMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /** Node: yalnız aktif kayıtlar, pageCode'a göre sıralı. */
    @Transactional(readOnly = true)
    public List<DeeplinkPageDto> list(UUID appId) {
        return repository.findByAppIdAndIsActiveTrueOrderByPageCodeAsc(appId).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    public DeeplinkPageDto create(UUID appId, DeeplinkPageRequest req) {
        // Node'da bu iki alan Zod şemasında zorunlu; eksikse 400 VALIDATION_ERROR.
        if (req.name() == null || req.pageCode() == null) {
            throw new AppException("VALIDATION_ERROR", "Validation failed", 400, null);
        }

        DeeplinkPageEntity e = new DeeplinkPageEntity();
        e.setId(UUID.randomUUID());
        e.setAppId(appId);
        e.setName(req.name());
        e.setDescription(req.description());
        e.setPageCode(req.pageCode());
        // Zod default'ları: platform 'ios', paramSchema [], isActive true
        e.setPlatform(req.platform() == null ? "ios" : req.platform());
        e.setParamSchema(req.paramSchema() == null ? new ArrayList<>() : new ArrayList<>(req.paramSchema()));
        e.setActive(req.isActive() == null || req.isActive());

        Instant now = Instant.now();
        e.setCreatedAt(now);
        e.setUpdatedAt(now);

        return mapper.toDto(repository.save(e));
    }

    @Transactional
    public DeeplinkPageDto update(UUID appId, UUID id, DeeplinkPageRequest req) {
        DeeplinkPageEntity e = repository.findByIdAndAppId(id, appId)
                .orElseThrow(() -> AppException.notFound("Deeplink page not found"));

        if (req.name() != null) {
            e.setName(req.name());
        }
        if (req.description() != null) {
            e.setDescription(req.description());
        }
        if (req.pageCode() != null) {
            e.setPageCode(req.pageCode());
        }
        if (req.platform() != null) {
            e.setPlatform(req.platform());
        }
        if (req.paramSchema() != null) {
            e.setParamSchema(new ArrayList<>(req.paramSchema()));
        }
        if (req.isActive() != null) {
            e.setActive(req.isActive());
        }
        e.setUpdatedAt(Instant.now());

        return mapper.toDto(repository.save(e));
    }

    /** Node 404 ATMAZ — kayıt yoksa da 204 döner. */
    @Transactional
    public void delete(UUID appId, UUID id) {
        repository.findByIdAndAppId(id, appId).ifPresent(repository::delete);
    }
}
