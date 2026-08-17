package tr.com.agesa.appinsight.common.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Zaman damgası parite testi.
 *
 * <p>Bu testin varlık sebebi: PostgreSQL mikrosaniye tutar, Node'un pg sürücüsü
 * milisaniyeye kırpar. Java tarafı kırpmazsa contract test'lerde her tarih alanı
 * sapar — ve bu, tek tek endpoint testlerinde "neden 3 harf fazla" diye aranan
 * türden bir hatadır.
 */
class InstantMillisSerializerTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new AppInsightJsonModule());

    @Test
    void mikrosaniyeyiMilisaniyeyeKirpar() throws Exception {
        // Postgres'ten gelen tipik değer: 2026-04-22 11:46:28.822024+00
        Instant micros = Instant.parse("2026-04-22T11:46:28.822024Z");

        assertThat(mapper.writeValueAsString(micros)).isEqualTo("\"2026-04-22T11:46:28.822Z\"");
    }

    @Test
    void kesirSifirsaUcBasamakYazar() throws Exception {
        // JS toISOString() her zaman .000Z yazar; Java'nın ISO_INSTANT'ı hiç yazmaz.
        Instant whole = Instant.parse("2026-04-22T11:46:28Z");

        assertThat(mapper.writeValueAsString(whole)).isEqualTo("\"2026-04-22T11:46:28.000Z\"");
    }

    @Test
    void sondakiSifirlariKorur() throws Exception {
        Instant trailingZero = Instant.parse("2026-04-22T11:46:28.820Z");

        assertThat(mapper.writeValueAsString(trailingZero)).isEqualTo("\"2026-04-22T11:46:28.820Z\"");
    }
}
