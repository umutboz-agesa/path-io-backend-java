package tr.com.agesa.appinsight.admin.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tr.com.agesa.appinsight.admin.client.dto.ActivityResponse;
import tr.com.agesa.appinsight.admin.client.dto.DataResponse;
import tr.com.agesa.appinsight.admin.client.dto.DeviceDto;
import tr.com.agesa.appinsight.admin.client.dto.SessionDetailResponse;
import tr.com.agesa.appinsight.admin.client.dto.SessionDto;
import tr.com.agesa.appinsight.admin.service.DeviceService;

import java.util.UUID;

/**
 * Cihaz, oturum ve activity uçları — Node'daki {@code devicesRoutes} karşılığı.
 *
 * <p>{@code sessionId} yol değişkeni {@code String}'dir, {@code UUID} değil — değeri SDK
 * üretiyor ve UUID formatında olmayabilir.
 */
@RestController
@RequestMapping("/api/v1")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping("/apps/{appId}/activity")
    public ActivityResponse activity(@PathVariable UUID appId,
                                     @RequestParam(required = false) Integer limit,
                                     @RequestParam(required = false) Integer offset,
                                     @RequestParam(required = false) String deviceId) {
        return deviceService.activity(appId, limit, offset, deviceId);
    }

    @GetMapping("/apps/{appId}/devices")
    public DataResponse<DeviceDto> devices(@PathVariable UUID appId) {
        return DataResponse.of(deviceService.devices(appId));
    }

    @GetMapping("/apps/{appId}/devices/{deviceId}/sessions")
    public DataResponse<SessionDto> deviceSessions(@PathVariable UUID appId, @PathVariable String deviceId) {
        return DataResponse.of(deviceService.sessionsByDevice(appId, deviceId));
    }

    @GetMapping("/apps/{appId}/sessions")
    public DataResponse<SessionDto> sessions(@PathVariable UUID appId) {
        return DataResponse.of(deviceService.sessionsByApp(appId));
    }

    @GetMapping("/apps/{appId}/sessions/{sessionId}")
    public SessionDetailResponse sessionDetail(@PathVariable UUID appId, @PathVariable String sessionId) {
        return deviceService.sessionDetail(appId, sessionId);
    }
}
