package tr.com.agesa.appinsight.admin.client.dto;

import java.time.Instant;
import java.util.List;

/**
 * {@code GET /api/v1/apps/:appId/screens} yanıtı.
 *
 * <p>Ekranlar hem düz liste ({@code screens}) hem de kanonik ada göre gruplanmış
 * ({@code groups}) olarak döner — portal ikisini de kullanıyor.
 */
public record ScreenListResponse(List<ScreenDto> screens, List<Group> groups) {

    /**
     * Kanonik ad altında toplanmış ekranlar.
     *
     * @param canonicalName gruplama anahtarı — {@code canonicalName} yoksa {@code displayName}
     * @param platforms     gruptaki boş olmayan platformlar, üyelerin sırasına göre tekilleştirilmiş
     * @param totalEvents   üyelerin {@code eventCount} toplamı
     * @param lastSeenAt    bkz. ScreenService — Node'daki seçim mantığı birebir korunuyor
     */
    public record Group(
            String canonicalName,
            List<String> platforms,
            long totalEvents,
            Instant lastSeenAt,
            List<ScreenDto> members
    ) {
    }
}
