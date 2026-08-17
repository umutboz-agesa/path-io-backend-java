package tr.com.agesa.appinsight.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * {@code gcl_queries} tablosu — Google Cloud Logging filtre ifadeleri.
 */
@Entity
@Table(name = "gcl_queries")
@Getter
@Setter
public class GclQueryEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "app_id", nullable = false)
    private UUID appId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    /** GCL filtre ifadesi (severity, type, jsonPayload alanları). */
    @Column(name = "expression", nullable = false)
    private String expression;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
