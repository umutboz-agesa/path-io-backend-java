package tr.com.agesa.appinsight.admin.domain;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.net.InetSocketAddress;
import java.net.Socket;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Entity eşleme testlerinin yerel varyantı: geliştiricinin makinesinde <b>çalışan</b>
 * veritabanına karşı koşar — yani Node'un migration'larını gerçekten uygulamış olan şemaya.
 *
 * <p>Docker soketine erişemeyen ortamlarda (Testcontainers çalışmıyorsa) eşlemeyi doğrulamanın
 * yolu budur. Postgres 5432'de yoksa test atlanır, kırmızı yanmaz.
 *
 * <p>Testler {@code @DataJpaTest} sayesinde transaction içinde koşar ve <b>geri alınır</b> —
 * canlı geliştirme verisine kalıcı satır yazılmaz.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("local")
class LocalDbEntityMappingTest extends AbstractEntityMappingTests {

    @BeforeAll
    static void veritabaniAyaktaMi() {
        assumeTrue(portAcik("localhost", 5432),
                "localhost:5432 kapalı — yerel şema doğrulaması atlandı (bash start.sh ile açılır)");
    }

    private static boolean portAcik(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
