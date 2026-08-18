package tr.com.agesa.appinsight.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Worker yapılandırması.
 *
 * <p><b>Varsayılanlar bilerek gölge (shadow) moddur:</b> ayrı consumer group ve {@code dryRun}
 * açık. Böylece yanlış yapılandırmayla başlatılan bir Java worker'ı canlı Node akışından
 * mesaj çalamaz ve TimescaleDB'ye çift kayıt yazamaz.
 *
 * <p>Cutover'da {@code consumer-group: worker-main} ve {@code dry-run: false} yapılır.
 *
 * @param consumerGroup Redis consumer group. ASLA PID bazlı isim kullanma — zombie consumer birikir.
 * @param blockMs       XREADGROUP BLOCK süresi (Node: eventProcessor 2000ms).
 */
@ConfigurationProperties("appinsight.worker")
public record WorkerProperties(
        List<String> appIds,
        String consumerGroup,
        String consumerName,
        boolean dryRun,
        long blockMs
) {

    public WorkerProperties {
        appIds = appIds == null ? List.of() : appIds;
        consumerGroup = consumerGroup == null ? "event-processors-shadow" : consumerGroup;
        consumerName = consumerName == null ? "worker-shadow" : consumerName;
        blockMs = blockMs <= 0 ? 2000 : blockMs;
    }
}
