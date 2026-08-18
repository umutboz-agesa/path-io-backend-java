package tr.com.agesa.appinsight.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tr.com.agesa.appinsight.admin.domain.IntegrationEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IntegrationRepository extends JpaRepository<IntegrationEntity, UUID> {

    List<IntegrationEntity> findByAppId(UUID appId);

    Optional<IntegrationEntity> findByIdAndAppId(UUID id, UUID appId);

    Optional<IntegrationEntity> findByAppIdAndType(UUID appId, String type);
}
