package tr.com.agesa.appinsight.admin.client.dto;

import java.util.List;

/**
 * {@code { "data": [...] }} — meta'sız liste yanıtı.
 *
 * <p>Node'da bazı uçlar sayfalı ({@link PagedResponse}), bazıları yalnızca {@code data}
 * döner. Tutarsız görünse de portal her ucu ayrı ayrı bu şekle göre okuyor; birleştirilmemeli.
 */
public record DataResponse<T>(List<T> data) {

    public static <T> DataResponse<T> of(List<T> data) {
        return new DataResponse<>(data);
    }
}
