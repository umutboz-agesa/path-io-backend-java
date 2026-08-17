package tr.com.agesa.appinsight.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * {@code gcl_query_hits} tablosu — bir GCL sorgusunun eşleştiği olaylar.
 *
 * <p>{@code ts} olayın GCL'deki zamanı ({@code created_at} ise kaydın yazılma zamanı) —
 * ikisi karıştırılmamalı, geçmiş ekranları {@code ts} üzerinden sıralıyor.
 */
@Entity
@Table(name = "gcl_query_hits")
@Getter
@Setter
public class GclQueryHitEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "query_id", nullable = false)
    private UUID queryId;

    @Column(name = "app_id", nullable = false)
    private UUID appId;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "ts", nullable = false)
    private Instant ts;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_data", nullable = false)
    private Map<String, Object> eventData = new LinkedHashMap<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
