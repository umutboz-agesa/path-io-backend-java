package tr.com.agesa.appinsight.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tr.com.agesa.appinsight.admin.domain.DeeplinkPageEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeeplinkPageRepository extends JpaRepository<DeeplinkPageEntity, UUID> {

    /** Node: isActive=true filtresi + pageCode'a göre sıralı. */
    List<DeeplinkPageEntity> findByAppIdAndIsActiveTrueOrderByPageCodeAsc(UUID appId);

    Optional<DeeplinkPageEntity> findByIdAndAppId(UUID id, UUID appId);
}
