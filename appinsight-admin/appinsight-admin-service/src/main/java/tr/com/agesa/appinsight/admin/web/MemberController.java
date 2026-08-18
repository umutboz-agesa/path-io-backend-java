package tr.com.agesa.appinsight.admin.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tr.com.agesa.appinsight.admin.client.dto.AppMemberDto;
import tr.com.agesa.appinsight.admin.client.dto.DataResponse;
import tr.com.agesa.appinsight.admin.repository.AppMemberRepository;
import tr.com.agesa.appinsight.admin.service.AppMapper;

import java.util.List;
import java.util.UUID;

/**
 * {@code GET /api/v1/apps/:appId/members} — SDK'nın kaydettiği UI elemanları.
 * Opsiyonel {@code ?screen=} filtresi.
 *
 * <p>Tek uçlu ve iş mantığı olmayan bir kaynak; ayrı servis sınıfı açılmadı.
 * Node'da da sıralama YOK.
 */
@RestController
@RequestMapping("/api/v1")
public class MemberController {

    private final AppMemberRepository memberRepository;
    private final AppMapper mapper;

    public MemberController(AppMemberRepository memberRepository, AppMapper mapper) {
        this.memberRepository = memberRepository;
        this.mapper = mapper;
    }

    @GetMapping("/apps/{appId}/members")
    public DataResponse<AppMemberDto> list(@PathVariable UUID appId,
                                           @RequestParam(required = false) String screen) {
        List<AppMemberDto> rows = (screen != null
                ? memberRepository.findByAppIdAndScreen(appId, screen)
                : memberRepository.findByAppId(appId))
                .stream().map(mapper::toDto).toList();
        return DataResponse.of(rows);
    }
}
