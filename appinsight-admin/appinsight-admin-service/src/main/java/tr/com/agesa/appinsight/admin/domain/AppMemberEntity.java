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
 * {@code app_members} tablosu — unique(app_id, key, platform).
 *
 * <p>SDK'nın {@code member_register} WS mesajıyla bildirdiği UI elemanları (UITextField vb.).
 * {@code set_value} aksiyonu bu kayıtlar üzerinden hedef alan seçiyor.
 */
@Entity
@Table(name = "app_members")
@Getter
@Setter
public class AppMemberEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "app_id", nullable = false)
    private UUID appId;

    @Column(name = "key", nullable = false)
    private String key;

    @Column(name = "label")
    private String label;

    @Column(name = "element_type", nullable = false)
    private String elementType = "input";

    @Column(name = "screen", nullable = false)
    private String screen;

    @Column(name = "platform", nullable = false)
    private String platform = "ios";

    @Column(name = "last_registered_at", nullable = false)
    private Instant lastRegisteredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
