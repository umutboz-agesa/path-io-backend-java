package tr.com.agesa.appinsight.admin.repository;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import tr.com.agesa.appinsight.admin.domain.SessionEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<SessionEntity, String> {

    /** Node: sessionService.getByDevice — startedAt DESC, varsayılan limit 20. */
    List<SessionEntity> findByAppIdAndDeviceIdOrderByStartedAtDesc(UUID appId, String deviceId, Limit limit);

    /** Node: sessionService.getByApp — startedAt DESC, varsayılan limit 50. */
    List<SessionEntity> findByAppIdOrderByStartedAtDesc(UUID appId, Limit limit);

    Optional<SessionEntity> findByIdAndAppId(String id, UUID appId);
}
