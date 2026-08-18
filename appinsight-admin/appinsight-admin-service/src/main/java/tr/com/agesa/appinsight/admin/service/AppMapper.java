package tr.com.agesa.appinsight.admin.service;

import org.springframework.stereotype.Component;
import tr.com.agesa.appinsight.admin.client.dto.AppDto;
import tr.com.agesa.appinsight.admin.client.dto.DeeplinkPageDto;
import tr.com.agesa.appinsight.admin.client.dto.ScreenDto;
import tr.com.agesa.appinsight.admin.client.dto.TemplateDto;
import tr.com.agesa.appinsight.admin.domain.AppEntity;
import tr.com.agesa.appinsight.admin.domain.DeeplinkPageEntity;
import tr.com.agesa.appinsight.admin.domain.PayloadTemplateEntity;
import tr.com.agesa.appinsight.admin.domain.ScreenEntity;

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
