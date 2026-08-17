package tr.com.agesa.appinsight.common.json;

import com.fasterxml.jackson.databind.module.SimpleModule;

import java.time.Instant;

/**
 * Node ile JSON parite'sini sağlayan Jackson modülü.
 * Servislerin {@code ObjectMapper}'ına kaydedilir.
 */
public class AppInsightJsonModule extends SimpleModule {

    public AppInsightJsonModule() {
        super("AppInsightJsonModule");
        addSerializer(Instant.class, new InstantMillisSerializer());
    }
}
