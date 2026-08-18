package tr.com.agesa.appinsight.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tr.com.agesa.appinsight.admin.domain.InsightEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InsightRepository extends JpaRepository<InsightEntity, UUID> {

    List<InsightEntity> findByAppIdOrderByCreatedAtDesc(UUID appId);

    Optional<InsightEntity> findByIdAndAppId(UUID id, UUID appId);
}
