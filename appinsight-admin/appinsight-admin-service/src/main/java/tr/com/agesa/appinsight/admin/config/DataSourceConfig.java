package tr.com.agesa.appinsight.admin.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Ana (PostgreSQL) veri kaynağı — <b>açıkça</b> tanımlanır.
 *
 * <p><b>Neden elle tanımlanıyor:</b> Spring Boot'un {@code DataSourceAutoConfiguration}'ı
 * {@code @ConditionalOnMissingBean(DataSource.class)} ile çalışır. TimescaleDB için ikinci bir
 * {@code DataSource} bean'i tanımlandığı anda auto-config <b>tamamen geri çekilir</b> ve ana
 * veri kaynağı hiç oluşturulmaz; JPA sessizce Timescale havuzuna bağlanır. Yani "ayrı havuz"
 * kurduğunu sanırken tek havuz kalır — bu hata bir kez yapıldı ve log'da yalnız
 * {@code appinsight-timescale-pool} açıldığı görülerek yakalandı.
 *
 * <p>Bu sınıf ana veri kaynağını {@code @Primary} olarak açıkça kurar; böylece JPA ve
 * {@code jdbcTemplate} bunu, zaman serisi sorguları ise {@link TimescaleConfig}'deki havuzu
 * kullanır. İki havuzun da açıldığı başlangıç log'undan doğrulanabilir.
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
}
