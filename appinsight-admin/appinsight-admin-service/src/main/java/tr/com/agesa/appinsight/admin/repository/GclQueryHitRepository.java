package tr.com.agesa.appinsight.admin.repository;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tr.com.agesa.appinsight.admin.domain.GclQueryHitEntity;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface GclQueryHitRepository extends JpaRepository<GclQueryHitEntity, UUID> {

    List<GclQueryHitEntity> findByQueryIdAndAppIdOrderByTsDesc(UUID queryId, UUID appId, Limit limit);

    @Modifying
    @Query("delete from GclQueryHitEntity h where h.queryId = :queryId and h.appId = :appId")
    void deleteAllForQuery(@Param("queryId") UUID queryId, @Param("appId") UUID appId);

    @Modifying
    @Query("delete from GclQueryHitEntity h where h.queryId = :queryId and h.appId = :appId and h.id in :ids")
    void deleteByIds(@Param("queryId") UUID queryId,
                     @Param("appId") UUID appId,
                     @Param("ids") Collection<UUID> ids);
}
