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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@code deeplink_pages} tablosu — unique(app_id, page_code, platform).
 *
 * <p>{@code pageCode} iOS tarafındaki {@code RedirectionPageModel} ham değeridir (ör. 101).
 * {@code paramSchema} jsonb <b>dizi</b>: {@code [{ key, type, required, label, defaultValue? }]}.
 */
@Entity
@Table(name = "deeplink_pages")
@Getter
@Setter
public class DeeplinkPageEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "app_id", nullable = false)
    private UUID appId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "page_code", nullable = false)
    private int pageCode;

    @Column(name = "platform", nullable = false)
    private String platform = "ios";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "param_schema", nullable = false)
    private List<Map<String, Object>> paramSchema = new ArrayList<>();

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
