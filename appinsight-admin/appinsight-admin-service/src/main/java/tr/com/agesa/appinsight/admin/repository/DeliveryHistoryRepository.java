package tr.com.agesa.appinsight.admin.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import tr.com.agesa.appinsight.admin.domain.InsightDeliveryEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Funnel teslimat geçmişi — serbest {@code limit}/{@code offset} gerektirdiği için
 * {@code Pageable} yerine {@code EntityManager} kullanılıyor.
 *
 * <p>{@code Pageable} sayfa numarası üzerinden çalışır ve {@code offset}'in {@code limit}'in
 * katı olmasını şart koşar; Node ise ikisini bağımsız alıyor (ör. limit=100&offset=37).
 * Sayfalamayı zorlamak parite kaybı olurdu.
 */
@Repository
public class DeliveryHistoryRepository {

    @PersistenceContext
    private EntityManager em;

    public record Filters(UUID funnelId, UUID appId, String status, String userAction, String deviceId) {
    }

    public List<InsightDeliveryEntity> find(Filters f, int limit, int offset) {
        TypedQuery<InsightDeliveryEntity> query = em.createQuery(
                "select d from InsightDeliveryEntity d " + where(f) + " order by d.deliveredAt desc",
                InsightDeliveryEntity.class);
        bind(query, f);
        query.setFirstResult(Math.max(offset, 0));
        query.setMaxResults(Math.max(limit, 0));
        return query.getResultList();
    }

    public long count(Filters f) {
        TypedQuery<Long> query = em.createQuery(
                "select count(d) from InsightDeliveryEntity d " + where(f), Long.class);
        bind(query, f);
        return query.getSingleResult();
    }

    /**
     * Node'daki filtre mantığı:
     * <ul>
     *   <li>{@code status} verildiyse eşitlik</li>
     *   <li>{@code userAction == 'none'} ise NULL olanlar; başka bir değerse eşitlik</li>
     *   <li>{@code deviceId} tam eşleşme değil, {@code LIKE %…%}</li>
     * </ul>
     */
    private static String where(Filters f) {
        List<String> clauses = new ArrayList<>();
        clauses.add("d.funnelId = :funnelId");
        clauses.add("d.appId = :appId");
        if (f.status() != null) {
            clauses.add("d.status = :status");
        }
        if ("none".equals(f.userAction())) {
            clauses.add("d.userAction is null");
        } else if (f.userAction() != null) {
            clauses.add("d.userAction = :userAction");
        }
        if (f.deviceId() != null) {
            clauses.add("d.deviceId like :deviceId");
        }
        return "where " + String.join(" and ", clauses);
    }

    private static void bind(TypedQuery<?> query, Filters f) {
        query.setParameter("funnelId", f.funnelId());
        query.setParameter("appId", f.appId());
        if (f.status() != null) {
            query.setParameter("status", f.status());
        }
        if (f.userAction() != null && !"none".equals(f.userAction())) {
            query.setParameter("userAction", f.userAction());
        }
        if (f.deviceId() != null) {
            query.setParameter("deviceId", "%" + f.deviceId() + "%");
        }
    }
}
