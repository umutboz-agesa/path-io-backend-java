package tr.com.agesa.appinsight.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.com.agesa.appinsight.admin.client.dto.IntegrationDto;
import tr.com.agesa.appinsight.admin.client.dto.IntegrationRequest;
import tr.com.agesa.appinsight.admin.domain.IntegrationEntity;
import tr.com.agesa.appinsight.admin.repository.AppRepository;
import tr.com.agesa.appinsight.admin.repository.IntegrationRepository;
import tr.com.agesa.appinsight.common.error.AppException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@code backend/src/api/routes/integrations.ts} karşılığı.
 *
 * <p>Şu an tek entegrasyon tipi var: {@code gcl} (Google Cloud Logging → Pub/Sub).
 */
@Service
public class IntegrationService {

    private static final String MASK = "••••••••";

    private final IntegrationRepository repository;
    private final AppRepository appRepository;

    public IntegrationService(IntegrationRepository repository, AppRepository appRepository) {
        this.repository = repository;
        this.appRepository = appRepository;
    }

    @Transactional(readOnly = true)
    public List<IntegrationDto> list(UUID appId) {
        assertAppExists(appId);
        return repository.findByAppId(appId).stream().map(IntegrationService::toMaskedDto).toList();
    }

    @Transactional
    public IntegrationDto create(UUID appId, IntegrationRequest req) {
        assertAppExists(appId);

        // Node createIntegrationSchema: type enum ['gcl'], config ve credentials ZORUNLU.
        if (!"gcl".equals(req.type()) || req.config() == null || req.credentials() == null) {
            throw validationFailed();
        }
        validateConfig(req.config());
        validateCredentials(req.credentials());
        repository.findByAppIdAndType(appId, req.type()).ifPresent(existing -> {
            throw new AppException("CONFLICT", "Integration already exists for this type", 409, null);
        });

        IntegrationEntity e = new IntegrationEntity();
        e.setId(UUID.randomUUID());
        e.setAppId(appId);
        e.setType(req.type());
        e.setConfig(new LinkedHashMap<>(req.config()));
        e.setCredentials(new LinkedHashMap<>(req.credentials()));
        e.setActive(req.isActive() != null && req.isActive());   // Node default: false
        e.setStatus("pending");

        Instant now = Instant.now();
        e.setCreatedAt(now);
        e.setUpdatedAt(now);

        return toMaskedDto(repository.save(e));
    }

    /**
     * Node'da bu uç {@code PUT} ama kısmi güncelleme yapıyor. Ayrıca her güncellemede
     * {@code status} 'pending'e, {@code lastError} null'a çekilir — ayar değişince bağlantı
     * yeniden test edilmeli sayılıyor.
     */
    @Transactional
    public IntegrationDto update(UUID appId, UUID id, IntegrationRequest req) {
        // Node updateIntegrationSchema: alanlar opsiyonel AMA verilirse tam şemaya uymalı.
        // Kısmi bir credentials nesnesi 400 döner — parça parça güncelleme YOK.
        // ({@code type} alanı update şemasında yok; gönderilse de Zod tarafından atılır.)
        if (req.config() != null) {
            validateConfig(req.config());
        }
        if (req.credentials() != null) {
            validateCredentials(req.credentials());
        }

        IntegrationEntity e = find(appId, id);

        if (req.config() != null) {
            e.setConfig(new LinkedHashMap<>(req.config()));
        }
        if (req.credentials() != null) {
            e.setCredentials(new LinkedHashMap<>(req.credentials()));
        }
        if (req.isActive() != null) {
            e.setActive(req.isActive());
        }
        e.setStatus("pending");
        e.setLastError(null);
        e.setUpdatedAt(Instant.now());

        return toMaskedDto(repository.save(e));
    }

    @Transactional
    public void delete(UUID appId, UUID id) {
        repository.delete(find(appId, id));
    }

    @Transactional(readOnly = true)
    public IntegrationEntity findForTest(UUID appId, UUID id) {
        return find(appId, id);
    }

    @Transactional
    public void markStatus(UUID id, String status, String lastError) {
        repository.findById(id).ifPresent(e -> {
            e.setStatus(status);
            e.setLastError(lastError);
            e.setUpdatedAt(Instant.now());
            repository.save(e);
        });
    }

    private IntegrationEntity find(UUID appId, UUID id) {
        return repository.findByIdAndAppId(id, appId)
                .orElseThrow(() -> AppException.notFound("Integration not found"));
    }

    /**
     * Node'daki {@code assertAppExists} DEĞİL — bu route {@code isActive} filtresi
     * KULLANMIYOR, yalnız kaydın varlığına bakıyor. Soft-delete edilmiş bir app'in
     * entegrasyonları hâlâ listelenebiliyor. Tuhaf ama davranış korunuyor.
     */
    private void assertAppExists(UUID appId) {
        if (appRepository.findById(appId).isEmpty()) {
            throw AppException.notFound("App not found");
        }
    }

    // ── Doğrulama — Node'daki Zod şemalarının karşılığı ──────────────────────

    /** {@code gclConfigSchema}: projectId ve subscriptionName zorunlu, logFilter opsiyonel. */
    private static void validateConfig(Map<String, Object> config) {
        requireNonEmptyString(config.get("projectId"));
        requireNonEmptyString(config.get("subscriptionName"));
    }

    /**
     * {@code gclCredentialsSchema}: servis hesabı anahtarının tam yapısı. {@code passthrough()}
     * olduğu için fazladan alanlar serbest, ama listedekiler eksik olamaz.
     *
     * <p><b>Sapma:</b> Zod'un {@code .email()} / {@code .url()} regex'leri ile buradaki basit
     * kontroller uç örneklerde ayrışabilir (ör. tuhaf ama teknik olarak geçerli adresler).
     * Gerçek servis hesabı anahtarlarında fark üretmez.
     */
    private static void validateCredentials(Map<String, Object> creds) {
        if (!"service_account".equals(creds.get("type"))) {
            throw validationFailed();
        }
        for (String field : List.of("project_id", "private_key_id", "private_key", "client_id")) {
            requireNonEmptyString(creds.get(field));
        }
        String email = requireNonEmptyString(creds.get("client_email"));
        if (!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw validationFailed();
        }
        for (String field : List.of("auth_uri", "token_uri")) {
            String url = requireNonEmptyString(creds.get(field));
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                throw validationFailed();
            }
        }
    }

    private static String requireNonEmptyString(Object value) {
        if (!(value instanceof String s) || s.isEmpty()) {
            throw validationFailed();
        }
        return s;
    }

    private static AppException validationFailed() {
        return new AppException("VALIDATION_ERROR", "Validation failed", 400, null);
    }

    /**
     * Servis hesabı anahtarını maskeler.
     *
     * <p>Node: {@code const { private_key, private_key_id, ...safe } = creds} ardından
     * {@code { ...safe, private_key, private_key_id }} — yani maskelenen iki alan
     * <b>nesnenin SONUNA</b> taşınır. Alan sırası JSON çıktısında görünür olduğu için
     * bu davranış birebir üretiliyor (LinkedHashMap: önce diğerleri, sonra bu ikisi).
     *
     * <p>Boş {@code credentials} maskelenmez, {@code {}} olarak döner.
     */
    static Map<String, Object> maskCredentials(Map<String, Object> creds) {
        if (creds == null || creds.isEmpty()) {
            return new LinkedHashMap<>();
        }

        Map<String, Object> masked = new LinkedHashMap<>();
        creds.forEach((k, v) -> {
            if (!"private_key".equals(k) && !"private_key_id".equals(k)) {
                masked.put(k, v);
            }
        });

        masked.put("private_key", MASK);
        String keyId = String.valueOf(creds.getOrDefault("private_key_id", ""));
        // Node: String(private_key_id ?? '').slice(0, 8) + '••••••••'
        masked.put("private_key_id", keyId.substring(0, Math.min(8, keyId.length())) + MASK);
        return masked;
    }

    private static IntegrationDto toMaskedDto(IntegrationEntity e) {
        return new IntegrationDto(
                e.getId().toString(),
                e.getAppId().toString(),
                e.getType(),
                e.getConfig(),
                maskCredentials(e.getCredentials()),
                e.isActive(),
                e.getStatus(),
                e.getLastError(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
