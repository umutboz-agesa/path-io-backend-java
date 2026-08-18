package tr.com.agesa.appinsight.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tr.com.agesa.appinsight.admin.domain.PayloadTemplateEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayloadTemplateRepository extends JpaRepository<PayloadTemplateEntity, UUID> {

    List<PayloadTemplateEntity> findByAppId(UUID appId);

    /** Tenant izolasyonu: id tek başına yeterli değil, app_id ile birlikte aranır. */
    Optional<PayloadTemplateEntity> findByIdAndAppId(UUID id, UUID appId);
}
