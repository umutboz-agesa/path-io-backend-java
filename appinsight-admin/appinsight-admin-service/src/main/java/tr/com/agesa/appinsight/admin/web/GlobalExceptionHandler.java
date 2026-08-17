package tr.com.agesa.appinsight.admin.web;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tr.com.agesa.appinsight.common.error.ApiErrorResponse;
import tr.com.agesa.appinsight.common.error.AppException;

import java.util.List;
import java.util.Map;

/**
 * Node'daki {@code server.setErrorHandler(...)} karşılığı — hata gövdesi şekli korunur.
 *
 * <p><b>Bilinen sapma:</b> Node'da doğrulama hatasının {@code details} alanı Zod'un
 * {@code err.errors} dizisidir ({@code code/path/message/expected/received} alanları).
 * Java tarafında Bean Validation ihlalleri {@code path/message} olarak eşlenir; kod ve
 * mesaj metinleri birebir aynı değildir. Web portal bu alanı göstermiyor, contract test'te
 * {@code details} içeriği karşılaştırma dışı bırakılmalıdır.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiErrorResponse> handleAppException(AppException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(ApiErrorResponse.of(ex.getCode(), ex.getMessage(), ex.getDetails()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleBodyValidation(MethodArgumentNotValidException ex) {
        List<Map<String, Object>> details = ex.getBindingResult().getFieldErrors().stream()
                .<Map<String, Object>>map(fe -> Map.of(
                        "path", List.of(fe.getField()),
                        "message", String.valueOf(fe.getDefaultMessage())))
                .toList();
        return ResponseEntity.status(400)
                .body(ApiErrorResponse.of("VALIDATION_ERROR", "Validation failed", details));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleParamValidation(ConstraintViolationException ex) {
        List<Map<String, Object>> details = ex.getConstraintViolations().stream()
                .<Map<String, Object>>map(v -> Map.of(
                        "path", List.of(pathOf(v)),
                        "message", v.getMessage()))
                .toList();
        return ResponseEntity.status(400)
                .body(ApiErrorResponse.of("VALIDATION_ERROR", "Validation failed", details));
    }

    /** Gövde hiç parse edilemediğinde (bozuk JSON) — Node'da da 400 döner. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(400)
                .body(ApiErrorResponse.of("VALIDATION_ERROR", "Validation failed", null));
    }

    /** Geçersiz UUID gibi path/param tip hataları. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(400)
                .body(ApiErrorResponse.of("BAD_REQUEST", "Bad request", null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(500)
                .body(ApiErrorResponse.of("INTERNAL_ERROR", "Internal server error", null));
    }

    private static String pathOf(ConstraintViolation<?> v) {
        String full = v.getPropertyPath().toString();
        int dot = full.indexOf('.');
        return dot >= 0 ? full.substring(dot + 1) : full;
    }
}
