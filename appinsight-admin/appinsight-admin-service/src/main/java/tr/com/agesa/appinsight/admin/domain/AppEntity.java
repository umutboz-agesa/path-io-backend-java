package tr.com.agesa.appinsight.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * {@code apps} tablosu.
 *
 * <p><b>Şemaya dokunulmaz</b> — tablo Drizzle migration'ları ile yönetilir,
 * Hibernate {@code ddl-auto: none} ile çalışır. Bu sınıf yalnızca mevcut şemayı okur/yazar.
 *
 * <p>jsonb kolonlar {@link JdbcTypeCode} + {@link SqlTypes#JSON} ile eşlenir; ek kütüphane
 * (hypersistence-utils) gerekmez, Hibernate 6 bunu yerleşik destekler.
 */
@Entity
@Table(name = "apps")
public class AppEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "api_key", nullable = false, unique = true)
    private String apiKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "bundle_ids", nullable = false)
    private Map<String, Object> bundleIds = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "platforms", nullable = false)
    private String[] platforms = new String[0];

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", nullable = false)
    private Map<String, Object> config = new LinkedHashMap<>();

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Map<String, Object> getBundleIds() {
        return bundleIds;
    }

    public void setBundleIds(Map<String, Object> bundleIds) {
        this.bundleIds = bundleIds;
    }

    public String[] getPlatforms() {
        return platforms;
    }

    public void setPlatforms(String[] platforms) {
        this.platforms = platforms;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
