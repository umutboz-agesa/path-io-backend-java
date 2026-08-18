package tr.com.agesa.appinsight.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tr.com.agesa.appinsight.admin.domain.GclQueryEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GclQueryRepository extends JpaRepository<GclQueryEntity, UUID> {

    List<GclQueryEntity> findByAppIdOrderByCreatedAtDesc(UUID appId);

    Optional<GclQueryEntity> findByIdAndAppId(UUID id, UUID appId);
}
