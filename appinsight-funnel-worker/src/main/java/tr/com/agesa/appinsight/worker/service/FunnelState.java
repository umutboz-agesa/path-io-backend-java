package tr.com.agesa.appinsight.worker.service;

import java.util.Map;

/**
 * {@code funnel_state:{funnelId}:{deviceId}} Redis hash'inin karşılığı.
 *
 * <p>Alan adları Redis'te snake_case tutulur ({@code current_step}, {@code step_entered_at}) —
 * Node ile aynı hash'i okuyup yazabilmek için birebir korunmalı.
 *
 * @param currentStep   1'den başlar; tamamlanınca {@code steps.size()}'ı aşar
 * @param startedAt     funnel'ın ilk adımının eşleştiği an (global timeout buradan sayılır)
 * @param stepEnteredAt mevcut adıma girildiği an (adım timeout'u buradan sayılır)
 */
public record FunnelState(
        String funnelId,
        String deviceId,
        int currentStep,
        long startedAt,
        long stepEnteredAt,
        boolean completed,
        Map<String, Object> gclData
) {

    public FunnelState advancedTo(int step, long ts) {
        return new FunnelState(funnelId, deviceId, step, startedAt, ts, completed, gclData);
    }
}
