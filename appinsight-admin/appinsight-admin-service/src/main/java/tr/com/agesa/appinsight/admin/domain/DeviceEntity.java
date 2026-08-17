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
 * {@code devices} tablosu — unique(app_id, device_id).
 *
 * <p>{@code device_id} SDK'nın ürettiği UUID string'dir; tablonun kendi {@code id}'si ile
 * karıştırılmamalı. Redis anahtarlarında ve WS mesajlarında geçen "deviceId" bu kolondur.
 */
@Entity
@Table(name = "devices")
@Getter
@Setter
public class DeviceEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "app_id", nullable = false)
    private UUID appId;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "platform", nullable = false)
    private String platform;

    @Column(name = "os_version", nullable = false)
    private String osVersion;

    @Column(name = "app_version", nullable = false)
    private String appVersion;

    @Column(name = "model", nullable = false)
    private String model;

    @Column(name = "last_seen", nullable = false)
    private Instant lastSeen;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false)
    private Map<String, Object> metadata = new LinkedHashMap<>();
}
