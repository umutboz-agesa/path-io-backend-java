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
 * {@code integrations} tablosu — unique(app_id, type). Şimdilik tek tip: {@code gcl}.
 *
 * <p><b>Güvenlik:</b> {@code credentials} kolonu GCP servis hesabı anahtarını tutar.
 * Bu alan REST yanıtlarında DÖNÜLMEMELİ — DTO'ya asla doğrudan kopyalanmamalı
 * (Faz 1'de integrations uçları yazılırken dikkat).
 */
@Entity
@Table(name = "integrations")
@Getter
@Setter
public class IntegrationEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "app_id", nullable = false)
    private UUID appId;

    @Column(name = "type", nullable = false)
    private String type = "gcl";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", nullable = false)
    private Map<String, Object> config = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "credentials", nullable = false)
    private Map<String, Object> credentials = new LinkedHashMap<>();

    @Column(name = "is_active", nullable = false)
    private boolean isActive = false;

    /** pending | connected | error */
    @Column(name = "status", nullable = false)
    private String status = "pending";

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
