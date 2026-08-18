package tr.com.agesa.appinsight.admin.service;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.com.agesa.appinsight.admin.client.dto.GclQueryDto;
import tr.com.agesa.appinsight.admin.client.dto.GclQueryHitDto;
import tr.com.agesa.appinsight.admin.client.dto.GclQueryRequest;
import tr.com.agesa.appinsight.admin.domain.GclQueryEntity;
import tr.com.agesa.appinsight.admin.repository.GclQueryHitRepository;
import tr.com.agesa.appinsight.admin.repository.GclQueryRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * {@code backend/src/api/routes/gclQueries.ts} karşılığı.
 *
 * <p>Bu route dosyası projedeki diğerlerinden ayrışıyor: Zod doğrulaması yok, güncelleme
 * {@code PUT} (PATCH değil) ve 404 gövdesi {@code AppError} zarfı yerine {@code {"error":"Not found"}}.
 * Üçü de korunuyor.
 */
@Service
public class GclQueryService {

    /** Node: hits ucunda varsayılan limit '100'. */
    private static final int DEFAULT_HIT_LIMIT = 100;

    private final GclQueryRepository queryRepository;
    private final GclQueryHitRepository hitRepository;
    private final AppMapper mapper;

    public GclQueryService(GclQueryRepository queryRepository,
                           GclQueryHitRepository hitRepository,
                           AppMapper mapper) {
        this.queryRepository = queryRepository;
        this.hitRepository = hitRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<GclQueryDto> list(UUID appId) {
        return queryRepository.findByAppIdOrderByCreatedAtDesc(appId).stream().map(mapper::toDto).toList();
    }

    @Transactional
    public GclQueryDto create(UUID appId, GclQueryRequest req) {
        GclQueryEntity e = new GclQueryEntity();
        e.setId(UUID.randomUUID());
        e.setAppId(appId);
        e.setName(req.name());
        e.setDescription(req.description());
        e.setExpression(req.expression());
        e.setActive(req.isActive() == null || req.isActive());   // Node: isActive ?? true

        Instant now = Instant.now();
        e.setCreatedAt(now);
        e.setUpdatedAt(now);

        return mapper.toDto(queryRepository.save(e));
    }

    /**
     * Node'da bu uç {@code PUT} ve gövdeyi {@code Partial<>} olarak alıyor — yani PUT olmasına
     * rağmen kısmi güncelleme yapıyor. Kayıt yoksa {@code null} döner; controller 404 üretir.
     */
    @Transactional
    public Optional<GclQueryDto> update(UUID appId, UUID id, GclQueryRequest req) {
        return queryRepository.findByIdAndAppId(id, appId).map(e -> {
            if (req.name() != null) {
                e.setName(req.name());
            }
            if (req.description() != null) {
                e.setDescription(req.description());
            }
            if (req.expression() != null) {
                e.setExpression(req.expression());
            }
            if (req.isActive() != null) {
                e.setActive(req.isActive());
            }
            e.setUpdatedAt(Instant.now());
            return mapper.toDto(queryRepository.save(e));
        });
    }

    /** Node 404 ATMAZ — kayıt yoksa da 204. */
    @Transactional
    public void delete(UUID appId, UUID id) {
        queryRepository.findByIdAndAppId(id, appId).ifPresent(queryRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<GclQueryHitDto> hits(UUID appId, UUID queryId, Integer limit) {
        return hitRepository
                .findByQueryIdAndAppIdOrderByTsDesc(queryId, appId,
                        Limit.of(limit == null ? DEFAULT_HIT_LIMIT : limit))
                .stream().map(mapper::toDto).toList();
    }

    /** Gövdede {@code ids} varsa yalnız onlar, yoksa sorgunun tüm hit'leri silinir. */
    @Transactional
    public void deleteHits(UUID appId, UUID queryId, Collection<UUID> ids) {
        if (ids != null && !ids.isEmpty()) {
            hitRepository.deleteByIds(queryId, appId, ids);
        } else {
            hitRepository.deleteAllForQuery(queryId, appId);
        }
    }
}
