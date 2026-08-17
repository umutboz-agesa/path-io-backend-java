package tr.com.agesa.appinsight.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Hata gövdesi — Node'daki {@code reply.send({ error: { code, message, details } })} ile aynı şekil.
 *
 * <p>{@code details} null ise alan JSON'a hiç yazılmaz; Node tarafında
 * {@code details: undefined} JSON.stringify sırasında düştüğü için davranış birebir aynıdır.
 */
public record ApiErrorResponse(Body error) {

    public static ApiErrorResponse of(String code, String message, Object details) {
        return new ApiErrorResponse(new Body(code, message, details));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Body(String code, String message, Object details) {
    }
}
