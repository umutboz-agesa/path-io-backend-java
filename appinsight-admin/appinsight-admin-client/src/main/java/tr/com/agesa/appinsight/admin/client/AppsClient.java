package tr.com.agesa.appinsight.admin.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import tr.com.agesa.appinsight.admin.client.dto.AppDto;
import tr.com.agesa.appinsight.admin.client.dto.CreateAppRequest;
import tr.com.agesa.appinsight.admin.client.dto.PagedResponse;
import tr.com.agesa.appinsight.admin.client.dto.SdkConfigDto;
import tr.com.agesa.appinsight.admin.client.dto.UpdateAppRequest;

/**
 * Diğer mini-service'lerin admin servisine erişimi.
 *
 * <p>Örn. realtime servisi bir SDK bağlantısında api_key doğrulaması için app kaydını
 * bu arayüz üzerinden çeker.
 */
@FeignClient(name = "appinsight-admin", url = "${appinsight.admin.url}", path = "/api/v1")
public interface AppsClient {

    @GetMapping("/apps")
    PagedResponse<AppDto> list(@RequestParam(required = false) Integer page,
                               @RequestParam(required = false) Integer limit,
                               @RequestParam(required = false) String search);

    @GetMapping("/apps/{id}")
    AppDto get(@PathVariable String id);

    @PostMapping("/apps")
    AppDto create(@RequestBody CreateAppRequest request);

    @PatchMapping("/apps/{id}")
    AppDto update(@PathVariable String id, @RequestBody UpdateAppRequest request);

    @DeleteMapping("/apps/{id}")
    ResponseEntity<Void> delete(@PathVariable String id);

    @GetMapping("/apps/{id}/sdk-config")
    SdkConfigDto sdkConfig(@PathVariable String id);
}
