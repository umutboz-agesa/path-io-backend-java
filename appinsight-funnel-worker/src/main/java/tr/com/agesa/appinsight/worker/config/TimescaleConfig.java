package tr.com.agesa.appinsight.worker.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * TimescaleDB için ayrı havuz — admin servisindeki ile aynı gerekçe: zaman serisi yazımları
 * ana havuzu bloklamamalı (Node'da da ayrı {@code Pool} var).
 */
@Configuration
public class TimescaleConfig {

    @Bean
    @ConfigurationProperties("appinsight.timescale")
    public DataSource timescaleDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean
    public JdbcTemplate timescaleJdbcTemplate(@Qualifier("timescaleDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
