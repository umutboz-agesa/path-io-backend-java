package tr.com.agesa.appinsight.admin.service;

import org.springframework.stereotype.Component;
import tr.com.agesa.appinsight.admin.client.dto.AppDto;
import tr.com.agesa.appinsight.admin.client.dto.ScreenDto;
import tr.com.agesa.appinsight.admin.domain.AppEntity;
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
