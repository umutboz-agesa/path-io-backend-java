package tr.com.agesa.appinsight.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tr.com.agesa.appinsight.admin.domain.AppMemberEntity;

import java.util.List;
import java.util.UUID;

public interface AppMemberRepository extends JpaRepository<AppMemberEntity, UUID> {

    List<AppMemberEntity> findByAppId(UUID appId);

    List<AppMemberEntity> findByAppIdAndScreen(UUID appId, String screen);
}
