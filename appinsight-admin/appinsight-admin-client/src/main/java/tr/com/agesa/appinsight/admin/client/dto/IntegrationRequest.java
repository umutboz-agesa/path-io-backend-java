package tr.com.agesa.appinsight.admin.client.dto;

import java.util.Map;

/**
 * Entegrasyon oluşturma/güncelleme gövdesi.
 *
 * <p>Node'da POST tam şemayı ({@code type}, {@code config}, {@code credentials} zorunlu),
 * PUT ise hepsi opsiyonel bir şemayı kullanır. Zorunluluk kontrolü servis katmanında yapılır.
 *
 * <p>{@code credentials} GCP servis hesabı anahtarıdır — loglanmamalı, yanıta ham hâliyle
 * konulmamalıdır.
 */
public record IntegrationRequest(
        String type,
        Map<String, Object> config,
        Map<String, Object> credentials,
        Boolean isActive
) {
}
