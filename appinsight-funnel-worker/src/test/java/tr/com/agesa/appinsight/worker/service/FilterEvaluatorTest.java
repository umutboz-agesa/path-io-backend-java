package tr.com.agesa.appinsight.worker.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Filtre motoru parite testi.
 *
 * <p><b>Beklenen değerler uydurulmadı</b> — Node'daki {@code filterEvaluator.ts} canlı
 * çalıştırılıp çıktıları alındı ve buraya gömüldü. Yani bu test "Java kendi kendine tutarlı mı"
 * değil, "Java Node ile aynı kararı veriyor mu" sorusunu ölçüyor.
 */
class FilterEvaluatorTest {

    private final FilterEvaluator evaluator = new FilterEvaluator();

    private static Map<String, String> context() {
        Map<String, String> ctx = new LinkedHashMap<>();
        ctx.put("platform", "ios");
        ctx.put("appVersion", "3.6.0");
        ctx.put("osVersion", "18.1");
        ctx.put("model", "iPhone16,2");
        return ctx;
    }

    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @CsvSource(delimiter = '|', value = {
            "platform = \"ios\"                                              | true",
            "platform = \"android\"                                          | false",
            "platform != \"android\"                                         | true",
            "appVersion >= \"3.6.0\"                                         | true",
            "appVersion > \"3.6.0\"                                          | false",
            "appVersion > \"3.10.0\"                                         | false",
            "appVersion < \"3.10.0\"                                         | true",
            "osVersion >= \"18\"                                             | true",
            "platform = \"ios\" AND appVersion >= \"3.6.0\"                  | true",
            "platform = \"android\" AND appVersion >= \"3.6.0\"              | false",
            "platform = \"android\" OR appVersion >= \"3.6.0\"               | true",
            "platform = \"android\" OR platform = \"web\"                    | false",
            "bilinmeyenAlan = \"x\"                                          | false",
            "bilinmeyenAlan = \"\"                                           | true",
            "bozuk ifade $$$                                                 | false",
            "platform                                                        | false",
            "platform = 'ios'                                                | true",
            "platform = ios                                                  | true",
            "platform = \"ios\" AND platform = \"android\" OR appVersion = \"3.6.0\" | true",
    })
    @DisplayName("ifadeler Node ile aynı sonucu veriyor")
    void ifadelerNodeIleAyniSonucVeriyor(String expression, boolean expected) {
        assertThat(evaluator.evaluate(expression.trim(), context())).isEqualTo(expected);
    }

    @Test
    @DisplayName("boş ifade true döner (Node: token yok → boş alan = boş değer)")
    void bosIfadeTrueDoner() {
        assertThat(evaluator.evaluate("", context())).isTrue();
    }

    @Test
    @DisplayName("sayı olmayan sürümle karşılaştırma HER YÖNDE false (NaN yayılımı)")
    void nanKarsilastirmasiHerYondeFalse() {
        // Node: cmpVersion("3.6.0","abc") → NaN; NaN >= 0 ve NaN < 0 ikisi de false.
        // Bu, "büyük değilse küçüktür" sezgisinin bozulduğu tek yer.
        assertThat(evaluator.evaluate("appVersion >= \"abc\"", context())).isFalse();
        assertThat(evaluator.evaluate("appVersion < \"abc\"", context())).isFalse();
        assertThat(evaluator.evaluate("appVersion > \"abc\"", context())).isFalse();
        assertThat(evaluator.evaluate("appVersion <= \"abc\"", context())).isFalse();
    }

    @Test
    @DisplayName("flattenJson Node ile aynı düz haritayı üretiyor")
    void flattenJsonNodeIleAyni() {
        Map<String, Object> payload = new LinkedHashMap<>();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("payload", Map.of("value", 42));
        data.put("list", List.of(1, 2));
        data.put("flag", true);
        payload.put("data", data);
        payload.put("top", "x");
        payload.put("nil", null);
        payload.put("dec", 4.5);

        // Node çıktısı:
        // {"data.payload.value":"42","data.list":"1,2","data.flag":"true","top":"x","dec":"4.5"}
        assertThat(evaluator.flattenJson(payload))
                .containsEntry("data.payload.value", "42")
                .containsEntry("data.list", "1,2")
                .containsEntry("data.flag", "true")
                .containsEntry("top", "x")
                .containsEntry("dec", "4.5")
                .doesNotContainKey("nil")          // null atlanır
                .hasSize(5);
    }

    @Test
    @DisplayName("tamsayı değerli ondalık JS gibi '42' yazılır, '42.0' değil")
    void tamsayiDegerliOndalikJsGibiYazilir() {
        // Jackson jsonb'yi Double olarak çözebiliyor; Java varsayılanı "42.0" yazardı
        // ve filtre eşitliği ("... = \"42\"") sessizce tutmazdı.
        assertThat(evaluator.flattenJson(Map.of("n", 42.0d))).containsEntry("n", "42");
    }
}
