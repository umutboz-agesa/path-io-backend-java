package tr.com.agesa.appinsight.admin.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tr.com.agesa.appinsight.admin.client.dto.AppDto;
import tr.com.agesa.appinsight.admin.client.dto.PagedResponse;
import tr.com.agesa.appinsight.admin.config.JacksonConfig;
import tr.com.agesa.appinsight.admin.service.AppService;
import tr.com.agesa.appinsight.common.error.AppException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REST kontrat testi — Node backend'inden yakalanan gerçek yanıt şekilleri baz alınmıştır.
 *
 * <p>Karşılaştırma referansı (canlı Node, {@code GET /api/v1/apps}):
 * <pre>
 * {"data":[{"id":"…","name":"…","apiKey":"…","bundleIds":{},"platforms":["ios","android"],
 *           "config":{},"isActive":true,"createdAt":"2026-04-22T11:46:28.822Z",
 *           "updatedAt":"2026-04-22T11:46:53.945Z"}],
 *  "meta":{"page":1,"limit":20,"total":4}}
 * </pre>
 */
@WebMvcTest(AppController.class)
@Import(JacksonConfig.class)
class AppControllerContractTest {

    private static final UUID ID = UUID.fromString("737fb2df-6812-468d-9f70-ca1e70fed76f");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppService appService;

    private static AppDto sampleApp() {
        return new AppDto(
                ID.toString(),
                "My App Updated",
                "173c3d5aa31946390d93880a409e024608cadf1f413a59cdb14d04bb3b20617f",
                Map.of(),
                List.of("ios", "android"),
                Map.of("theme", "dark"),
                true,
                Instant.parse("2026-04-22T11:46:28.822024Z"),   // mikrosaniyeli — kırpılmalı
                Instant.parse("2026-04-22T11:46:53.945Z")
        );
    }

    @Test
    void listeYanitiDataVeMetaIleSarmalanir() throws Exception {
        given(appService.list(anyInt(), anyInt(), any()))
                .willReturn(PagedResponse.of(List.of(sampleApp()), 1, 20, 4));

        mockMvc.perform(get("/api/v1/apps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(ID.toString()))
                .andExpect(jsonPath("$.data[0].apiKey").exists())
                .andExpect(jsonPath("$.data[0].isActive").value(true))
                // Zaman damgası JS toISOString() formatında, mikrosaniye kırpılmış olmalı
                .andExpect(jsonPath("$.data[0].createdAt").value("2026-04-22T11:46:28.822Z"))
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.limit").value(20))
                .andExpect(jsonPath("$.meta.total").value(4));
    }

    @Test
    void tekilYanitSarmalanmaz() throws Exception {
        given(appService.get(ID)).willReturn(sampleApp());

        mockMvc.perform(get("/api/v1/apps/{id}", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void bulunamayanKayit404VeNodeIleAyniGovdeDoner() throws Exception {
        willThrow(AppException.notFound("App not found")).given(appService).get(eq(ID));

        mockMvc.perform(get("/api/v1/apps/{id}", ID))
                .andExpect(status().isNotFound())
                .andExpect(content().json("{\"error\":{\"code\":\"NOT_FOUND\",\"message\":\"App not found\"}}", true));
    }

    @Test
    void olusturma201Doner() throws Exception {
        given(appService.create(any())).willReturn(sampleApp());

        mockMvc.perform(post("/api/v1/apps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"platforms\":[\"ios\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.apiKey").exists());
    }

    @Test
    void gecersizGovde400VeValidationErrorDoner() throws Exception {
        mockMvc.perform(post("/api/v1/apps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"platforms\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("Validation failed"));
    }

    @Test
    void silme204VeBosGovdeDoner() throws Exception {
        mockMvc.perform(delete("/api/v1/apps/{id}", ID))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }
}
