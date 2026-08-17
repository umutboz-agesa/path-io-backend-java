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
 * {@code sessions} tablosu.
 *
 * <p><b>Dikkat:</b> Birincil anahtar {@code uuid} DEĞİL, {@code text}. Değeri SDK üretir
 * ({@code sdk_init} mesajındaki {@code session_id}) ve olduğu gibi saklanır. UUID'ye
 * çevrilmemeli — SDK farklı bir format göndermeye başlarsa satır yazılamaz hâle gelir.
 */
@Entity
@Table(name = "sessions")
@Getter
@Setter
public class SessionEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "app_id", nullable = false)
    private UUID appId;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "platform", nullable = false)
    private String platform = "";

    @Column(name = "app_version", nullable = false)
    private String appVersion = "";

    @Column(name = "os_version", nullable = false)
    private String osVersion = "";

    @Column(name = "model", nullable = false)
    private String model = "";

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    /** Oturum kapanmadıysa null. */
    @Column(name = "ended_at")
    private Instant endedAt;
}
