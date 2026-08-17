package tr.com.agesa.appinsight.admin.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * TimescaleDB için <b>ayrı bağlantı havuzu</b>.
 *
 * <p><b>Neden ayrı:</b> Node tarafında da öyle ({@code src/db/timescale/client.ts} — kendi
 * {@code Pool}'u, max 10). Zaman serisi sorguları uzun sürebiliyor; ana havuzu paylaşırlarsa
 * CRUD istekleri onların arkasında bekler. Bu ayrım geçmişte yaşanmış bir sorunun karşılığı,
 * kozmetik değil.
 *
 * <p><b>Not:</b> Şu an aynı veritabanına işaret ediyor ({@code TIMESCALE_URL == DATABASE_URL});
 * hypertable ayrı bir DB'de değil, aynı DB içinde. Ayrı DB'ye taşınırsa yalnızca
 * {@code appinsight.timescale.url} değişir, kod değişmez.
 *
 * <p>Zaman serisi sorguları JPA'ya sıkıştırılmaz — {@code DISTINCT ON}, {@code time_bucket}
 * gibi ifadeler için native SQL kullanılır (yol haritası §4).
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
