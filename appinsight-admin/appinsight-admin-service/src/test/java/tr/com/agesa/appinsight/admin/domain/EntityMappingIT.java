package tr.com.agesa.appinsight.admin.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.testcontainers.DockerClientFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Entity eşleme testlerinin CI varyantı: şema sıfırdan, gerçek Drizzle migration'ları ile kurulur.
 *
 * <p>Migration'lar {@code src/test/resources/db/migrations} altında tutulur ve
 * {@code scripts/sync-migrations.sh} ile Node repo'sundan tazelenir. Node tarafında yeni
 * migration eklenip bu script çalıştırılmazsa test ESKİ şemaya karşı geçmeye devam eder —
 * bilinçli kabul edilmiş bir risk.
 *
 * <p><b>Docker gerektirir.</b> Docker erişilemiyorsa Testcontainers testi başlatamaz;
 * bu durumda yerel doğrulama için {@link LocalDbEntityMappingTest} kullanılır.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@EnabledIf("dockerKullanilabilir")
class EntityMappingIT extends AbstractEntityMappingTests {

    /**
     * Docker erişilebilir değilse sınıf tamamen atlanır — hata vermez.
     * ExecutionCondition, Testcontainers eklentisinden ÖNCE değerlendirilir; bu yüzden
     * container başlatılmaya çalışılmadan devre dışı kalır.
     */
    static boolean dockerKullanilabilir() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (RuntimeException e) {
            return false;
        }
    }

    // docker-compose ile aynı imaj — 0001 migration'ındaki create_hypertable() için TimescaleDB şart.
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("timescale/timescaledb:latest-pg15")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("insight_platform")
            .withUsername("insight")
            .withPassword("password");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired
    private DataSource dataSource;

    private static boolean migrated = false;

    @BeforeEach
    void runDrizzleMigrations() throws Exception {
        if (migrated) {
            return;
        }
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            for (Path sql : migrationFiles()) {
                // Drizzle ifadeleri bu işaretle ayırır.
                for (String stmt : Files.readString(sql).split("--> statement-breakpoint")) {
                    if (!stmt.isBlank()) {
                        st.execute(stmt);
                    }
                }
            }
        }
        migrated = true;
    }

    private static List<Path> migrationFiles() {
        try {
            Path dir = Paths.get(EntityMappingIT.class.getResource("/db/migrations").toURI());
            try (Stream<Path> files = Files.list(dir)) {
                return files.filter(p -> p.toString().endsWith(".sql"))
                        .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                        .toList();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
