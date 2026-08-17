package tr.com.agesa.appinsight.admin.config;

import com.fasterxml.jackson.databind.Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tr.com.agesa.appinsight.common.json.AppInsightJsonModule;

@Configuration
public class JacksonConfig {

    /** Zaman damgalarını JS {@code toISOString()} formatına sabitler — Node ile parite. */
    @Bean
    public Module appInsightJsonModule() {
        return new AppInsightJsonModule();
    }
}
