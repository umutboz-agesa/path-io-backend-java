package tr.com.agesa.appinsight.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tr.com.agesa.appinsight.admin.domain.FunnelEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FunnelRepository extends JpaRepository<FunnelEntity, UUID> {

    /** Node'da bu listede sıralama YOK — fiziksel sıra döner. */
    List<FunnelEntity> findByAppId(UUID appId);

    Optional<FunnelEntity> findByIdAndAppId(UUID id, UUID appId);
}
