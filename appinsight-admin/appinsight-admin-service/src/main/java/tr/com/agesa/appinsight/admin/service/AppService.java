package tr.com.agesa.appinsight.admin.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.com.agesa.appinsight.admin.client.dto.AppDto;
import tr.com.agesa.appinsight.admin.client.dto.CreateAppRequest;
import tr.com.agesa.appinsight.admin.client.dto.PagedResponse;
import tr.com.agesa.appinsight.admin.client.dto.SdkConfigDto;
import tr.com.agesa.appinsight.admin.client.dto.UpdateAppRequest;
import tr.com.agesa.appinsight.admin.domain.AppEntity;
import tr.com.agesa.appinsight.admin.repository.AppRepository;
import tr.com.agesa.appinsight.admin.repository.ScreenRepository;
import tr.com.agesa.appinsight.common.error.AppException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.UUID;

/**
 * {@code backend/src/api/routes/apps.ts} iş mantığının birebir karşılığı.
 */
@Service
public class AppService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AppRepository appRepository;
    private final ScreenRepository screenRepository;
    private final AppMapper mapper;

    public AppService(AppRepository appRepository, ScreenRepository screenRepository, AppMapper mapper) {
        this.appRepository = appRepository;
        this.screenRepository = screenRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PagedResponse<AppDto> list(int page, int limit, String search) {
        // Node tarafında ORDER BY yok — sıralama Postgres'in döndürdüğü fiziksel sıradır.
        // Parite için burada da sıralama EKLENMEZ (PageRequest.of unsorted).
        Page<AppEntity> result = appRepository.search(search, PageRequest.of(page - 1, limit));
        return PagedResponse.of(
                result.getContent().stream().map(mapper::toDto).toList(),
                page,
                limit,
                result.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public AppDto get(UUID id) {
        return mapper.toDto(findActive(id));
    }

    @Transactional
    public AppDto create(CreateAppRequest req) {
        AppEntity e = new AppEntity();
        e.setId(UUID.randomUUID());
        e.setName(req.name());
        e.setApiKey(generateApiKey());
        e.setBundleIds(req.bundleIds() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(req.bundleIds()));
        e.setPlatforms(req.platforms().toArray(String[]::new));
        e.setConfig(req.config() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(req.config()));
        e.setActive(true);

        // Node'da created_at/updated_at kolon DEFAULT now() ile DB tarafında atanır.
        // JPA insert'te kolonları listelediği için default devreye girmez; uygulama saatiyle
        // set ediyoruz. Fark: DB saati ile uygulama saati ayrışırsa (ayrı pod/host) sapma olur.
        // Tek makinede ve UTC'de çalışırken pratikte fark yok, cutover öncesi doğrulanmalı.
        Instant now = Instant.now();
        e.setCreatedAt(now);
        e.setUpdatedAt(now);

        return mapper.toDto(appRepository.save(e));
    }

    @Transactional
    public AppDto update(UUID id, UpdateAppRequest req) {
        AppEntity e = findActive(id);

        // null = "gönderilmedi" → dokunma. Node'daki spread davranışıyla aynı.
        if (req.name() != null) {
            e.setName(req.name());
        }
        if (req.platforms() != null) {
            e.setPlatforms(req.platforms().toArray(String[]::new));
        }
        if (req.bundleIds() != null) {
            e.setBundleIds(new LinkedHashMap<>(req.bundleIds()));
        }
        if (req.config() != null) {
            e.setConfig(new LinkedHashMap<>(req.config()));
        }
        e.setUpdatedAt(Instant.now());

        return mapper.toDto(appRepository.save(e));
    }

    /** Soft delete — kayıt silinmez, is_active=false yapılır. */
    @Transactional
    public void delete(UUID id) {
        AppEntity e = findActive(id);
        e.setActive(false);
        e.setUpdatedAt(Instant.now());
        appRepository.save(e);
    }

    @Transactional(readOnly = true)
    public SdkConfigDto sdkConfig(UUID id) {
        AppEntity app = findActive(id);
        return new SdkConfigDto(
                app.getApiKey(),
                app.getConfig(),
                screenRepository.findByAppId(id).stream().map(mapper::toDto).toList()
        );
    }

    private AppEntity findActive(UUID id) {
        return appRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> AppException.notFound("App not found"));
    }

    /** Node: {@code randomBytes(32).toString('hex')} → 64 karakter hex. */
    private static String generateApiKey() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
