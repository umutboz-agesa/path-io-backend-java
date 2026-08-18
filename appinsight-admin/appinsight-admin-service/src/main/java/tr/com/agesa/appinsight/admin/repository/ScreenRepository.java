package tr.com.agesa.appinsight.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tr.com.agesa.appinsight.admin.domain.ScreenEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScreenRepository extends JpaRepository<ScreenEntity, UUID> {

    List<ScreenEntity> findByAppId(UUID appId);

    /**
     * Node'daki {@code orderBy(screens.canonicalName, screens.name)} karşılığı.
     * PostgreSQL'de ASC sıralamada NULL'lar sona gelir — canonicalName'i olmayan
     * ekranlar listenin sonunda çıkar. Sıra portalde görünür, korunmalı.
     */
    List<ScreenEntity> findByAppIdOrderByCanonicalNameAscNameAsc(UUID appId);

    Optional<ScreenEntity> findByIdAndAppId(UUID id, UUID appId);
}
