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
 * {@code insights} tablosu — beş ayrı jsonb kolonu ile şemanın en karmaşık tablosu.
 *
 * <p><b>{@code target_screens} jsonb'dir, text[] DEĞİL.</b> {@code apps.platforms} gibi
 * Postgres dizisi değil, JSON dizisidir ({@code '[]'::jsonb} default). Yanlış eşlenirse
 * okuma anında patlar — {@code @JdbcTypeCode(JSON)} ile eşlendi.
 *
 * <p>{@code frequency} default'u {@code {"max_per_device":1,"window_hours":0}} — insightEngine
 * frekans penceresini buradan okur (Faz 4).
 *
 * <p>{@code action.type}: deeplink | url | dismiss | redirect | return_to | set_value.
 */
@Entity
@Table(name = "insights")
@Getter
@Setter
public class InsightEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "app_id", nullable = false)
    private UUID appId;

    /** Funnel silinirse SET NULL — insight kaydı korunur. */
    @Column(name = "funnel_id")
    private UUID funnelId;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "body", nullable = false)
    private String body = "";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "display", nullable = false)
    private Map<String, Object> display = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "action", nullable = false)
    private Map<String, Object> action = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "target", nullable = false)
    private Map<String, Object> target = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data", nullable = false)
    private Map<String, Object> data = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "target_screens", nullable = false)
    private List<String> targetScreens = new ArrayList<>();

    @Column(name = "gcl_data_step")
    private Integer gclDataStep;

    @Column(name = "status", nullable = false)
    private String status = "draft";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "frequency", nullable = false)
    private Map<String, Object> frequency = new LinkedHashMap<>();

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;
}
