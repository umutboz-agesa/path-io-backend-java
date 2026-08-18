package tr.com.agesa.appinsight.worker.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Ana (PostgreSQL) veri kaynağı ve {@code JdbcTemplate}'i — açıkça tanımlanır.
 *
 * <p>Gerekçe admin servisindekiyle aynı: ikinci bir {@code DataSource} bean'i (Timescale)
 * tanımlandığında Spring Boot'un datasource ve JdbcTemplate auto-config'i geri çekilir.
 * Elle tanımlanmazsa ana havuz hiç oluşmaz.
 */
@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
