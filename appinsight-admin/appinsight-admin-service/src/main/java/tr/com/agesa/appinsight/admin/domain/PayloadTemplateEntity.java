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
 * {@code payload_templates} tablosu.
 *
 * <p><b>İsim tuzağı:</b> Drizzle'da alan adı {@code fieldSchema} ama kolon adı {@code schema}.
 * Kolon adı {@code schema} olduğu için PostgreSQL'de tırnaklanması gerekir; Hibernate bunu
 * otomatik yapmaz, bu yüzden {@code @Column(name = "\"schema\"")} yazıldı. Tırnak
 * kaldırılırsa sorgu {@code schema} anahtar kelimesine takılır.
 */
@Entity
@Table(name = "payload_templates")
@Getter
@Setter
public class PayloadTemplateEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "app_id", nullable = false)
    private UUID appId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "\"schema\"", nullable = false)
    private Map<String, Object> fieldSchema = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "default_data", nullable = false)
    private Map<String, Object> defaultData = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "platforms", nullable = false)
    private String[] platforms = new String[0];

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
