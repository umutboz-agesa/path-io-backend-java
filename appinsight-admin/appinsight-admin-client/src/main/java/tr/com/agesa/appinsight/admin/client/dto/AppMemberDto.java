package tr.com.agesa.appinsight.admin.client.dto;

import java.time.Instant;

/**
 * {@code app_members} — SDK'nın {@code member_register} mesajıyla bildirdiği UI elemanları.
 * {@code set_value} aksiyonu hedef alanı buradan seçiyor.
 */
public record AppMemberDto(
        String id,
        String appId,
        String key,
        String label,
        String elementType,
        String screen,
        String platform,
        Instant lastRegisteredAt,
        Instant createdAt
) {
}
