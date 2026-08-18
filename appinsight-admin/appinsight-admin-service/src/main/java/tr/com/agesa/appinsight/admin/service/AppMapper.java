package tr.com.agesa.appinsight.admin.service;

import org.springframework.stereotype.Component;
import tr.com.agesa.appinsight.admin.client.dto.AppDto;
import tr.com.agesa.appinsight.admin.client.dto.AppMemberDto;
import tr.com.agesa.appinsight.admin.client.dto.DeeplinkPageDto;
import tr.com.agesa.appinsight.admin.client.dto.DeviceDto;
import tr.com.agesa.appinsight.admin.client.dto.FunnelDefinitionDto;
import tr.com.agesa.appinsight.admin.client.dto.FunnelDto;
import tr.com.agesa.appinsight.admin.client.dto.InsightDeliveryDto;
import tr.com.agesa.appinsight.admin.client.dto.InsightDto;
import tr.com.agesa.appinsight.admin.client.dto.GclQueryDto;
import tr.com.agesa.appinsight.admin.client.dto.GclQueryHitDto;
import tr.com.agesa.appinsight.admin.client.dto.ScreenDto;
import tr.com.agesa.appinsight.admin.client.dto.SessionDto;
import tr.com.agesa.appinsight.admin.client.dto.TemplateDto;
import tr.com.agesa.appinsight.admin.domain.AppEntity;
import tr.com.agesa.appinsight.admin.domain.AppMemberEntity;
import tr.com.agesa.appinsight.admin.domain.DeeplinkPageEntity;
import tr.com.agesa.appinsight.admin.domain.DeviceEntity;
import tr.com.agesa.appinsight.admin.domain.FunnelEntity;
import tr.com.agesa.appinsight.admin.domain.InsightDeliveryEntity;
import tr.com.agesa.appinsight.admin.domain.InsightEntity;
import tr.com.agesa.appinsight.admin.domain.GclQueryEntity;
import tr.com.agesa.appinsight.admin.domain.GclQueryHitEntity;
import tr.com.agesa.appinsight.admin.domain.PayloadTemplateEntity;
import tr.com.agesa.appinsight.admin.domain.ScreenEntity;
import tr.com.agesa.appinsight.admin.domain.SessionEntity;

import java.util.Arrays;
import java.util.Objects;

@Component
public class AppMapper {

    public AppDto toDto(AppEntity e) {
        return new AppDto(
                e.getId().toString(),
                e.getName(),
                e.getApiKey(),
                e.getBundleIds(),
                Arrays.asList(e.getPlatforms()),
                e.getConfig(),
                e.isActive(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    /** Ham satır temsili — POST/PATCH yanıtlarında. */
    public FunnelDto toDto(FunnelEntity e) {
        return new FunnelDto(
                e.getId().toString(),
                e.getAppId().toString(),
                e.getName(),
                e.getSteps(),
                e.getGlobalTimeoutMs(),
                e.getTargetFilter(),
                e.isActive(),
                e.getTriggerMode(),
                e.getStartsAt(),
                e.getExpiresAt(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    /** Motor temsili — GET liste yanıtında. Alan sırası {@link #toDto(FunnelEntity)}'dan farklı. */
    public FunnelDefinitionDto toDefinitionDto(FunnelEntity e) {
        return new FunnelDefinitionDto(
                e.getId().toString(),
                e.getAppId().toString(),
                e.getName(),
                e.getSteps(),
                e.getGlobalTimeoutMs(),
                e.getTargetFilter(),
                e.getTriggerMode(),
                e.isActive(),
                e.getStartsAt(),
                e.getExpiresAt(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    public InsightDto toDto(InsightEntity e) {
        return new InsightDto(
                e.getId().toString(),
                e.getAppId().toString(),
                Objects.toString(e.getFunnelId(), null),
                Objects.toString(e.getTemplateId(), null),
                e.getTitle(),
                e.getBody(),
                e.getDisplay(),
                e.getAction(),
                e.getTarget(),
                e.getData(),
                e.getTargetScreens(),
                e.getGclDataStep(),
                e.getStatus(),
                e.getFrequency(),
                e.getScheduledAt(),
                e.getCreatedAt(),
                e.getSentAt()
        );
    }

    public InsightDeliveryDto toDto(InsightDeliveryEntity e) {
        return new InsightDeliveryDto(
                e.getId().toString(),
                e.getInsightId().toString(),
                e.getAppId().toString(),
                e.getDeviceId(),
                e.getStatus(),
                Objects.toString(e.getFunnelId(), null),
                e.getTriggerCtx(),
                e.getDeliveredAt(),
                e.getUserAction(),
                e.getInteractedAt(),
                e.getActionClickedAt()
        );
    }

    public DeviceDto toDto(DeviceEntity e) {
        return new DeviceDto(
                e.getId().toString(),
                e.getAppId().toString(),
                e.getDeviceId(),
                e.getPlatform(),
                e.getOsVersion(),
                e.getAppVersion(),
                e.getModel(),
                e.getLastSeen(),
                e.getMetadata()
        );
    }

    public SessionDto toDto(SessionEntity e) {
        return new SessionDto(
                e.getId(),
                e.getAppId().toString(),
                e.getDeviceId(),
                e.getPlatform(),
                e.getAppVersion(),
                e.getOsVersion(),
                e.getModel(),
                e.getStartedAt(),
                e.getEndedAt()
        );
    }

    public AppMemberDto toDto(AppMemberEntity e) {
        return new AppMemberDto(
                e.getId().toString(),
                e.getAppId().toString(),
                e.getKey(),
                e.getLabel(),
                e.getElementType(),
                e.getScreen(),
                e.getPlatform(),
                e.getLastRegisteredAt(),
                e.getCreatedAt()
        );
    }

    public GclQueryDto toDto(GclQueryEntity e) {
        return new GclQueryDto(
                e.getId().toString(),
                e.getAppId().toString(),
                e.getName(),
                e.getDescription(),
                e.getExpression(),
                e.isActive(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    public GclQueryHitDto toDto(GclQueryHitEntity e) {
        return new GclQueryHitDto(
                e.getId().toString(),
                e.getQueryId().toString(),
                e.getAppId().toString(),
                e.getDeviceId(),
                e.getTs(),
                e.getEventData(),
                e.getCreatedAt()
        );
    }

    public TemplateDto toDto(PayloadTemplateEntity e) {
        return new TemplateDto(
                e.getId().toString(),
                e.getAppId().toString(),
                e.getName(),
                e.getDescription(),
                e.getFieldSchema(),
                e.getDefaultData(),
                Arrays.asList(e.getPlatforms()),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    public DeeplinkPageDto toDto(DeeplinkPageEntity e) {
        return new DeeplinkPageDto(
                e.getId().toString(),
                e.getAppId().toString(),
                e.getName(),
                e.getDescription(),
                e.getPageCode(),
                e.getPlatform(),
                e.getParamSchema(),
                e.isActive(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    public ScreenDto toDto(ScreenEntity e) {
        return new ScreenDto(
                e.getId().toString(),
                e.getAppId().toString(),
                Objects.toString(e.getParentId(), null),
                e.getName(),
                e.getDisplayName(),
                e.getPlatform(),
                e.getCanonicalName(),
                e.getFirstSeenAt(),
                e.getLastSeenAt(),
                e.getEventCount(),
                e.getMetadata(),
                e.getCreatedAt()
        );
    }
}
