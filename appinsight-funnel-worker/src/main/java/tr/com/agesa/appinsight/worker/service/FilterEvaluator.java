package tr.com.agesa.appinsight.worker.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code workers/filterEvaluator.ts}'in birebir karşılığı.
 *
 * <p>Funnel hedef filtrelerindeki ifadeleri değerlendirir:
 * {@code platform = "ios" AND appVersion >= "3.6.0"}.
 *
 * <p><b>Node'un davranışsal tuhaflıkları — bilerek korunuyor:</b>
 * <ul>
 *   <li>İfade ayrıştırılamazsa sonuç {@code true} olur (hata "geçir" yönünde). Bir yazım
 *       hatası filtreyi kapatmak yerine HERKESE açar; sürpriz ama davranış budur.</li>
 *   <li>{@code AND}/{@code OR} soldan sağa, aynı öncelikle işlenir — parantez desteği yok.</li>
 *   <li>Bilinmeyen alan boş string sayılır ({@code ctx[field] ?? ''}).</li>
 *   <li>Karşılaştırma operatörleri ({@code >}, {@code >=}, {@code <}, {@code <=}) sayısal
 *       değil <b>sürüm</b> karşılaştırması yapar: "3.10.0" &gt; "3.9.0" doğru sonuç verir.</li>
 * </ul>
 */
@Component
public class FilterEvaluator {

    private static final Pattern TOKEN = Pattern.compile(
            "\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*'|\\bAND\\b|\\bOR\\b|>=|<=|!=|[=><]|[\\w.]+");

    /**
     * İç içe JSON'u nokta notasyonlu düz haritaya çevirir.
     * {@code { data: { payload: { value: 42 } } }} → {@code { "data.payload.value": "42" }}
     *
     * <p>Diziler düzleştirilmez, {@code String.valueOf} ile metne çevrilir (Node'da da öyle).
     * {@code null} değerler atlanır.
     */
    public Map<String, String> flattenJson(Map<String, Object> source) {
        Map<String, String> result = new LinkedHashMap<>();
        flattenInto(source, "", result);
        return result;
    }

    @SuppressWarnings("unchecked")
    private void flattenInto(Map<String, Object> source, String prefix, Map<String, String> out) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            String path = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            if (value instanceof Map<?, ?> nested) {
                flattenInto((Map<String, Object>) nested, path, out);
            } else {
                out.put(path, stringify(value));
            }
        }
    }

    /**
     * JavaScript {@code String(val)} karşılığı. Tamsayı değerli ondalıklar için Java
     * {@code "42.0"} yazarken JS {@code "42"} yazar — bu fark filtre eşitliklerini bozardı.
     */
    private static String stringify(Object value) {
        if (value instanceof Double d && d == Math.floor(d) && !d.isInfinite()) {
            return String.valueOf(d.longValue());
        }
        if (value instanceof Float f && f == Math.floor(f) && !f.isInfinite()) {
            return String.valueOf(f.longValue());
        }
        if (value instanceof List<?> list) {
            List<String> parts = new ArrayList<>(list.size());
            for (Object item : list) {
                parts.add(item == null ? "" : stringify(item));
            }
            return String.join(",", parts);   // JS Array.toString()
        }
        return String.valueOf(value);
    }

    /** İfade değerlendirilemezse {@code true} döner — Node'daki {@code catch { return true }}. */
    public boolean evaluate(String expression, Map<String, String> context) {
        try {
            return new Parser(tokenize(expression), context).parseOr();
        } catch (RuntimeException e) {
            return true;
        }
    }

    private static List<String> tokenize(String expression) {
        List<String> tokens = new ArrayList<>();
        if (expression == null) {
            return tokens;
        }
        Matcher matcher = TOKEN.matcher(expression);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }

    private static final class Parser {
        private final List<String> tokens;
        private final Map<String, String> context;
        private int pos;

        Parser(List<String> tokens, Map<String, String> context) {
            this.tokens = tokens;
            this.context = context;
        }

        /** Node: {@code result = this.parseAnd() || result} — kısa devre YOK, iki taraf da işlenir. */
        boolean parseOr() {
            boolean result = parseAnd();
            while (peek() != null && "OR".equalsIgnoreCase(peek())) {
                consume();
                result = parseAnd() || result;
            }
            return result;
        }

        boolean parseAnd() {
            boolean result = parsePrimary();
            while (peek() != null && "AND".equalsIgnoreCase(peek())) {
                consume();
                result = parsePrimary() && result;
            }
            return result;
        }

        boolean parsePrimary() {
            String field = orEmpty(consume());
            String op = consume();
            if (op == null) {
                op = "=";
            }
            String raw = orEmpty(consume());
            String value = (raw.startsWith("\"") || raw.startsWith("'")) && raw.length() >= 2
                    ? raw.substring(1, raw.length() - 1)
                    : raw;
            String actual = context.getOrDefault(field, "");

            return switch (op) {
                case "=" -> actual.equals(value);
                case "!=" -> !actual.equals(value);
                case ">" -> compareVersion(actual, value) > 0;
                case ">=" -> compareVersion(actual, value) >= 0;
                case "<" -> compareVersion(actual, value) < 0;
                case "<=" -> compareVersion(actual, value) <= 0;
                default -> false;
            };
        }

        private String peek() {
            return pos < tokens.size() ? tokens.get(pos) : null;
        }

        private String consume() {
            return pos < tokens.size() ? tokens.get(pos++) : null;
        }

        private static String orEmpty(String value) {
            return value == null ? "" : value;
        }
    }

    /**
     * Sürüm karşılaştırması: "3.10.0" &gt; "3.9.0". Eksik parçalar 0 sayılır.
     *
     * <p><b>NaN davranışı kritik.</b> Node {@code Number("abc")} için {@code NaN} üretir;
     * {@code NaN - 3} yine {@code NaN} ve {@code NaN !== 0} <i>doğru</i> olduğu için fonksiyon
     * hemen {@code NaN} döndürür. Çağıran taraftaki {@code NaN >= 0} gibi tüm karşılaştırmalar
     * false olur — yani <b>sayı olmayan sürümle yapılan her karşılaştırma false</b>.
     * ({@code appVersion >= "abc"} → false, {@code appVersion < "abc"} → de false.)
     *
     * <p>Java'da {@code double} aritmetiği aynı NaN yayılımını yaptığı için mantık birebir
     * kopyalanabiliyor: dönüş tipi {@code double}, karşılaştırmalar çağıran tarafta.
     */
    static double compareVersion(String a, String b) {
        String[] pa = a.split("\\.");
        String[] pb = b.split("\\.");
        int length = Math.max(pa.length, pb.length);
        for (int i = 0; i < length; i++) {
            double left = parseNumber(i < pa.length ? pa[i] : null);
            double right = parseNumber(i < pb.length ? pb[i] : null);
            double diff = left - right;
            if (diff != 0) {          // NaN != 0 → true, NaN döner (JS ile aynı)
                return diff;
            }
        }
        return 0;
    }

    /** Node: {@code Number(part)} — eksik parça 0, boş string 0, çevrilemeyen NaN. */
    private static double parseNumber(String part) {
        if (part == null || part.isEmpty()) {
            return 0d;
        }
        try {
            return Double.parseDouble(part);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}
