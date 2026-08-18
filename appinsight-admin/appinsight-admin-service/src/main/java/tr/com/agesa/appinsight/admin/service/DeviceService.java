package tr.com.agesa.appinsight.admin.service;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.com.agesa.appinsight.admin.client.dto.ActivityResponse;
import tr.com.agesa.appinsight.admin.client.dto.DeviceDto;
import tr.com.agesa.appinsight.admin.client.dto.SessionDetailResponse;
import tr.com.agesa.appinsight.admin.client.dto.SessionDto;
import tr.com.agesa.appinsight.admin.domain.SessionEntity;
import tr.com.agesa.appinsight.admin.repository.ActivityRepository;
import tr.com.agesa.appinsight.admin.repository.AppRepository;
import tr.com.agesa.appinsight.admin.repository.DeviceRepository;
import tr.com.agesa.appinsight.admin.repository.SessionRepository;
import tr.com.agesa.appinsight.common.error.AppException;

import java.util.List;
import java.util.UUID;

/**
 * {@code backend/src/api/routes/devices.ts} + {@code services/sessionService.ts} okuma tarafı.
 */
@Service
public class DeviceService {

    /** Node: sessionService.getByDevice varsayılan limit. */
    private static final int DEVICE_SESSION_LIMIT = 20;
    /** Node: sessionService.getByApp varsayılan limit. */
    private static final int APP_SESSION_LIMIT = 50;

    private final DeviceRepository deviceRepository;
    private final SessionRepository sessionRepository;
    private final ActivityRepository activityRepository;
    private final AppRepository appRepository;
    private final AppMapper mapper;

    public DeviceService(DeviceRepository deviceRepository,
                         SessionRepository sessionRepository,
                         ActivityRepository activityRepository,
                         AppRepository appRepository,
                         AppMapper mapper) {
        this.deviceRepository = deviceRepository;
        this.sessionRepository = sessionRepository;
        this.activityRepository = activityRepository;
        this.appRepository = appRepository;
        this.mapper = mapper;
    }

    /**
     * Activity log. Cihaz filtresi varsa o cihazın son eventleri (limit/offset uygulanır),
     * yoksa cihaz başına SON event ({@code DISTINCT ON}) — bu varyantta Node limit/offset
     * UYGULAMAZ, yalnızca meta'da yankılar.
     */
    @Transactional(readOnly = true)
    public ActivityResponse activity(UUID appId, Integer limitParam, Integer offsetParam, String deviceId) {
        assertAppExists(appId);

        // Node: Math.min(Number(limit ?? 100), 500) — üst sınır 500, alt sınır kontrolü YOK.
        int limit = Math.min(limitParam == null ? 100 : limitParam, 500);
        int offset = offsetParam == null ? 0 : offsetParam;

        List<java.util.Map<String, Object>> rows = deviceId != null
                ? activityRepository.findByDevice(appId, deviceId, limit, offset)
                : activityRepository.findLastPerDevice(appId);

        return new ActivityResponse(rows, new ActivityResponse.Meta(limit, offset));
    }

    @Transactional(readOnly = true)
    public List<DeviceDto> devices(UUID appId) {
        assertAppExists(appId);
        return deviceRepository.findByAppIdOrderByLastSeenDesc(appId).stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<SessionDto> sessionsByDevice(UUID appId, String deviceId) {
        assertAppExists(appId);
        return sessionRepository
                .findByAppIdAndDeviceIdOrderByStartedAtDesc(appId, deviceId, Limit.of(DEVICE_SESSION_LIMIT))
                .stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<SessionDto> sessionsByApp(UUID appId) {
        assertAppExists(appId);
        return sessionRepository
                .findByAppIdOrderByStartedAtDesc(appId, Limit.of(APP_SESSION_LIMIT))
                .stream().map(mapper::toDto).toList();
    }

    /** Node: bu uçta assertAppExists YOK — oturum app_id ile birlikte aranıyor. */
    @Transactional(readOnly = true)
    public SessionDetailResponse sessionDetail(UUID appId, String sessionId) {
        SessionEntity session = sessionRepository.findByIdAndAppId(sessionId, appId)
                .orElseThrow(() -> AppException.notFound("Session not found"));

        return new SessionDetailResponse(
                mapper.toDto(session),
                activityRepository.screenTimeline(sessionId),
                activityRepository.eventList(sessionId));
    }

    private void assertAppExists(UUID appId) {
        appRepository.findByIdAndIsActiveTrue(appId)
                .orElseThrow(() -> AppException.notFound("App not found"));
    }
}
