package tr.com.agesa.appinsight.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tr.com.agesa.appinsight.admin.domain.ScreenEntity;

import java.util.List;
import java.util.UUID;

public interface ScreenRepository extends JpaRepository<ScreenEntity, UUID> {

    List<ScreenEntity> findByAppId(UUID appId);
}
