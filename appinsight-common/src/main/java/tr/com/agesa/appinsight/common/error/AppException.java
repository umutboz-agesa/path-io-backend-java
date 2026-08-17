package tr.com.agesa.appinsight.common.error;

/**
 * Node backend'indeki {@code AppError} sınıfının birebir karşılığı.
 *
 * <p>Hata gövdesi formatı korunur: {@code { "error": { "code", "message", "details"? } }}
 * — web portal ve SDK bu şekli bekliyor, değiştirilmemeli.
 *
 * @see <a href="file:../../../../../../../../backend/src/shared/errors.ts">backend/src/shared/errors.ts</a>
 */
public class AppException extends RuntimeException {

    private final String code;
    private final int statusCode;
    private final transient Object details;

    public AppException(String code, String message, int statusCode, Object details) {
        super(message);
        this.code = code;
        this.statusCode = statusCode;
        this.details = details;
    }

    public String getCode() {
        return code;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Object getDetails() {
        return details;
    }

    public static AppException notFound(String message) {
        return new AppException("NOT_FOUND", message, 404, null);
    }

    public static AppException badRequest(String message) {
        return new AppException("BAD_REQUEST", message, 400, null);
    }

    public static AppException unauthorized(String message) {
        return new AppException("UNAUTHORIZED", message, 401, null);
    }

    public static AppException forbidden(String message) {
        return new AppException("FORBIDDEN", message, 403, null);
    }

    public static AppException conflict(String message) {
        return new AppException("CONFLICT", message, 409, null);
    }

    public static AppException internal(String message) {
        return new AppException("INTERNAL_ERROR", message, 500, null);
    }
}
