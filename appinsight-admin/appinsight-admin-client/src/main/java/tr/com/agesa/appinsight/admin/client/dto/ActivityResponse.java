package tr.com.agesa.appinsight.admin.client.dto;

import java.util.List;
import java.util.Map;

/**
 * {@code GET /api/v1/apps/:appId/activity} yanıtı.
 *
 * <p>Satırlar ham SQL sonucudur — alan adları snake_case, sıra SELECT sırasıdır
 * (bkz. {@code ActivityRepository}). Tipli DTO'ya çevrilmedi çünkü portal bu adlara bağlı.
 *
 * <p>{@code meta} yanıtta her zaman bulunur; ancak cihaz filtresi YOKSA Node sorguya
 * limit/offset uygulamaz — değerler yalnızca yankılanır. Bu tuhaflık korunuyor.
 */
public record ActivityResponse(List<Map<String, Object>> data, Meta meta) {

    public record Meta(int limit, int offset) {
    }
}
