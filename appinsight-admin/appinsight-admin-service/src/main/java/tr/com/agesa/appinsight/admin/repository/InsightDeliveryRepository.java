package tr.com.agesa.appinsight.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tr.com.agesa.appinsight.admin.domain.InsightDeliveryEntity;

import java.util.List;
import java.util.UUID;

public interface InsightDeliveryRepository extends JpaRepository<InsightDeliveryEntity, UUID> {

    /** Node'da bu listede sıralama YOK. */
    List<InsightDeliveryEntity> findByInsightIdAndAppId(UUID insightId, UUID appId);
}
