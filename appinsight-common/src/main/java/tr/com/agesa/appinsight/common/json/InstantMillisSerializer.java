package tr.com.agesa.appinsight.common.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Zaman damgalarını JavaScript'in {@code Date.prototype.toISOString()} çıktısıyla
 * BİREBİR aynı formatta yazar: {@code 2026-04-22T11:46:28.822Z}.
 *
 * <p><b>Neden gerekli:</b> PostgreSQL {@code timestamptz} mikrosaniye hassasiyetinde tutar
 * (…:28.822024+00). Node'un {@code pg} sürücüsü bunu JS {@code Date}'e çevirir ve hassasiyet
 * milisaniyeye düşer → {@code .822Z}. Java tarafında {@code Instant} mikrosaniyeyi korur ve
 * Jackson varsayılanı {@code .822024Z} yazar — yani contract test'lerde alan alan fark oluşur.
 * Ayrıca ISO_INSTANT kesir kısmı sıfırsa hiç yazmaz ({@code …:28Z}), JS ise her zaman
 * {@code .000Z} yazar.
 *
 * <p>Bu serializer ikisini de kapatır: milisaniyeye kırpar ve kesir hanesini her zaman 3 basamak yazar.
 */
public class InstantMillisSerializer extends JsonSerializer<Instant> {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    @Override
    public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(FORMATTER.format(value));
    }
}
