package tr.com.agesa.appinsight.admin.client.dto;

import java.util.List;

/**
 * Node'daki liste yanıtı: {@code { data: [...], meta: { page, limit, total } }}.
 */
public record PagedResponse<T>(List<T> data, Meta meta) {

    public record Meta(int page, int limit, long total) {
    }

    public static <T> PagedResponse<T> of(List<T> data, int page, int limit, long total) {
        return new PagedResponse<>(data, new Meta(page, limit, total));
    }
}
