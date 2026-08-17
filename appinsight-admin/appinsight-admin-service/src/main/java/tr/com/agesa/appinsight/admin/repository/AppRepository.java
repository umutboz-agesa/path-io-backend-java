package tr.com.agesa.appinsight.admin.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tr.com.agesa.appinsight.admin.domain.AppEntity;

import java.util.Optional;
import java.util.UUID;

public interface AppRepository extends JpaRepository<AppEntity, UUID> {

    Optional<AppEntity> findByIdAndIsActiveTrue(UUID id);

    /**
     * Node'daki {@code and(eq(isActive,true), search ? ilike(name, '%s%') : undefined)} karşılığı.
     * {@code search} null ise filtre uygulanmaz — ilike büyük/küçük harf duyarsızdır.
     */
    @Query("""
            select a from AppEntity a
            where a.isActive = true
              and (:search is null or lower(a.name) like lower(concat('%', cast(:search as string), '%')))
            """)
    Page<AppEntity> search(@Param("search") String search, Pageable pageable);
}
