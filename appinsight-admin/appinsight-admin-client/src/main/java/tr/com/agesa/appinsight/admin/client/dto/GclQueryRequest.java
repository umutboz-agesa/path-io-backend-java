package tr.com.agesa.appinsight.admin.client.dto;

/**
 * GCL sorgusu oluşturma/güncelleme gövdesi.
 *
 * <p><b>Dikkat:</b> Node bu uçlarda Zod doğrulaması KULLANMIYOR — gövde doğrudan DB'ye
 * gidiyor. Bu yüzden burada da doğrulama annotation'ı yoktur; {@code name}/{@code expression}
 * eksikse hata veritabanının NOT NULL kısıtından gelir (500). Doğrulama eklemek daha "doğru"
 * olurdu ama Node ile farklı davranış üretirdi.
 */
public record GclQueryRequest(
        String name,
        String description,
        String expression,
        Boolean isActive
) {
}
