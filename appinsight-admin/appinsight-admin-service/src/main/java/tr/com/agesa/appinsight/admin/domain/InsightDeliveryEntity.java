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
 * {@code insight_deliveries} tablosu — teslimat ve kullanıcı aksiyonu geçmişi.
 *
 * <p>{@code userAction}: user_closed | auto_closed | permanent_dismiss.
 * {@code actionClickedAt} ayrı tutulur çünkü {@code action_clicked} hem Redis opt-out'u hem
 * SDK UserDefaults opt-out'unu tetikler — funnel geçmişi ekranı bu ayrımı gösteriyor.
 */
@Entity
@Table(name = "insight_deliveries")
@Getter
@Setter
public class InsightDeliveryEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "insight_id", nullable = false)
    private UUID insightId;

    @Column(name = "app_id", nullable = false)
    private UUID appId;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "status", nullable = false)
    private String status = "delivered";

    @Column(name = "funnel_id")
    private UUID funnelId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trigger_ctx", nullable = false)
    private Map<String, Object> triggerCtx = new LinkedHashMap<>();

    @Column(name = "delivered_at", nullable = false)
    private Instant deliveredAt;

    @Column(name = "user_action")
    private String userAction;

    @Column(name = "interacted_at")
    private Instant interactedAt;

    @Column(name = "action_clicked_at")
    private Instant actionClickedAt;
}
