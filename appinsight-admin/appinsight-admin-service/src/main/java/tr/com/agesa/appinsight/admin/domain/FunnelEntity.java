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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@code funnels} tablosu.
 *
 * <p>{@code steps} jsonb bir <b>dizi</b>dir (obje değil):
 * {@code [{ order, source, screen, matchType, conditions, timeoutMs }]}.
 * Faz 3'teki funnelMatcher bu yapıyı okuyacak — tip modeli oraya kadar {@code Map} olarak
 * bırakıldı, erken tipleme motor mantığından önce şema kararı vermek olurdu.
 *
 * <p>{@code triggerMode}: {@code session_once} | {@code screen_visit}.
 */
@Entity
@Table(name = "funnels")
@Getter
@Setter
public class FunnelEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "app_id", nullable = false)
    private UUID appId;

    @Column(name = "name", nullable = false)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "steps", nullable = false)
    private List<Map<String, Object>> steps = new ArrayList<>();

    @Column(name = "global_timeout_ms", nullable = false)
    private int globalTimeoutMs = 1_800_000;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "target_filter", nullable = false)
    private Map<String, Object> targetFilter = new LinkedHashMap<>();

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "trigger_mode", nullable = false)
    private String triggerMode = "session_once";

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
