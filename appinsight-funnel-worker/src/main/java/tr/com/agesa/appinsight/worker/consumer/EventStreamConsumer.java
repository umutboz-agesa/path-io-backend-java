package tr.com.agesa.appinsight.worker.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tr.com.agesa.appinsight.common.redis.RedisKeys;
import tr.com.agesa.appinsight.worker.config.WorkerProperties;
import tr.com.agesa.appinsight.worker.service.EventIngestService;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@code workers/eventProcessor.ts} karşılığı — {@code events:{appId}} stream'lerini tüketir.
 *
 * <h2>Consumer group ismi neden config'ten geliyor</h2>
 * Node'un tüketicisi {@code worker-main} adıyla çalışıyor. Java aynı isimle başlarsa Redis
 * mesajı <b>ikisinden yalnız birine</b> verir — yani Java, Node'un eventlerini çalar ve canlı
 * sistem bozulur. Paralel dönemde ayrı isim ({@code worker-shadow}) + {@code dry-run} kullanılır;
 * cutover'da {@code worker-main}'e geçilir ve dry-run kapatılır (yol haritası §6.2).
 *
 * <p>Bu yüzden varsayılan yapılandırma <b>gölge moddur</b>: ayrı grup, yazma yok.
 */
@Component
public class EventStreamConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventStreamConsumer.class);

    private final StringRedisTemplate redis;
    private final EventIngestService ingestService;
    private final WorkerProperties properties;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean running = new AtomicBoolean(true);

    private Thread worker;

    public EventStreamConsumer(StringRedisTemplate redis,
                               EventIngestService ingestService,
                               WorkerProperties properties,
                               ObjectMapper objectMapper) {
        this.redis = redis;
        this.ingestService = ingestService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (properties.appIds().isEmpty()) {
            log.warn("appinsight.worker.app-ids boş — tüketici başlatılmadı");
            return;
        }

        properties.appIds().forEach(this::ensureGroup);

        log.info("Event consumer başlıyor — group={} consumer={} dryRun={} appIds={}",
                properties.consumerGroup(), properties.consumerName(), properties.dryRun(), properties.appIds());

        worker = new Thread(this::loop, "event-stream-consumer");
        worker.setDaemon(true);
        worker.start();
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        if (worker != null) {
            worker.interrupt();
        }
    }

    /** Grup yoksa oluşturur. MKSTREAM: stream henüz yoksa birlikte yaratılır. */
    private void ensureGroup(String appId) {
        String key = RedisKeys.events(appId);
        try {
            redis.opsForStream().createGroup(key, ReadOffset.from("0"), properties.consumerGroup());
        } catch (RedisSystemException e) {
            // BUSYGROUP: grup zaten var — beklenen durum, sessiz geç.
            if (e.getMessage() == null || !e.getMessage().contains("BUSYGROUP")) {
                log.warn("Consumer group oluşturulamadı: {} ({})", key, e.getMessage());
            }
        }
    }

    private void loop() {
        List<StreamOffset<String>> offsets = properties.appIds().stream()
                .map(appId -> StreamOffset.create(RedisKeys.events(appId), ReadOffset.lastConsumed()))
                .toList();

        StreamReadOptions options = StreamReadOptions.empty()
                .count(50)
                .block(Duration.ofMillis(properties.blockMs()));

        Consumer consumer = Consumer.from(properties.consumerGroup(), properties.consumerName());

        while (running.get()) {
            try {
                List<MapRecord<String, Object, Object>> records = redis.opsForStream()
                        .read(consumer, options, offsets.toArray(StreamOffset[]::new));

                if (records == null || records.isEmpty()) {
                    continue;
                }
                for (MapRecord<String, Object, Object> record : records) {
                    handle(record);
                }
            } catch (Exception e) {
                if (!running.get()) {
                    return;
                }
                log.error("Event consumer döngü hatası", e);
                sleep(1000);
            }
        }
    }

    private void handle(MapRecord<String, Object, Object> record) {
        String streamKey = record.getStream();
        String appId = streamKey == null ? null : streamKey.substring(streamKey.indexOf(':') + 1);
        try {
            ingestService.process(toEvent(appId, record.getValue()), properties.dryRun());
            // Node ile aynı sıra: önce işle, sonra ack. İşlem patlarsa mesaj pending kalır.
            redis.opsForStream().acknowledge(streamKey, properties.consumerGroup(), record.getId());
        } catch (Exception e) {
            log.error("Event işlenemedi: appId={} id={}", appId, record.getId(), e);
        }
    }

    /** Stream alanları: device_id, session_id, screen, event, ts, duration, platform, props. */
    private EventIngestService.ScreenEvent toEvent(String appId, Map<Object, Object> fields) {
        return new EventIngestService.ScreenEvent(
                UUID.fromString(appId),
                str(fields.get("device_id")),
                emptyToNull(str(fields.get("session_id"))),
                str(fields.get("screen")),
                str(fields.get("event")),
                Instant.ofEpochMilli(Long.parseLong(str(fields.get("ts")))),
                parseDuration(str(fields.get("duration"))),
                emptyToNull(str(fields.get("platform"))),
                parseProps(str(fields.get("props"))));
    }

    /** Node: {@code Number(data.duration) || undefined} — 0 ve NaN ikisi de null olur. */
    private static Integer parseDuration(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            int value = (int) Double.parseDouble(raw);
            return value == 0 ? null : value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Map<String, Object> parseProps(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
