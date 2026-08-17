# Path.IO Backend — Java / Spring Boot

AppInsight platformunun Node.js/TypeScript backend'inin kurumsal Java standardına
taşınmış hâli. Hedef: **davranışsal olarak birebir aynı** sistem.

Yol haritası: `PathIO_Java_Donusum_Yol_Haritasi.docx` (ana repo kökü).

---

## Durum

| Faz | Kapsam | Durum |
|-----|--------|-------|
| 0 | Gradle iskelet, common lib, admin dikey dilimi (apps CRUD), Dockerfile + Jenkinsfile | 🟡 Kısmen |
| 1 | admin mini-service — kalan 45 endpoint | ⏳ Sırada |
| 2 | funnel-worker — eventProcessor (Redis Streams → TimescaleDB) | — |
| 3 | funnel-worker — funnelMatcher (550 satır durum makinesi) | — |
| 4 | realtime mini-service — WS ağ geçidi + insightEngine | — |
| 5 | gcl-bridge — Pub/Sub → Redis | — |
| 6 | Parity & cutover | — |

### Faz 0 — madde madde

Yol haritasındaki Faz 0 tanımına göre:

| Madde | Durum |
|-------|-------|
| Gradle multi-module + Spring Boot 3.5.6 + common lib | ✅ |
| İlk mini-service iskeleti (client/service ayrımı) | ✅ |
| Dikey dilim: apps CRUD, Java'da canlı ve parite doğrulanmış | ✅ |
| Dockerfile + Jenkinsfile | ⚠️ yazıldı, **çalıştırılmadı** (image build edilmedi, pipeline denenmedi) |
| 13 tablonun JPA entity + jsonb mapping'i | 🟡 **2/13** (apps, screens) |
| PostgreSQL bağlantısı | ✅ |
| Redis bağlantısı | ⚠️ starter + config var, **bağlantı doğrulanmadı** (henüz kullanan kod yok) |
| TimescaleDB ayrı pool | ❌ hiç kurulmadı |
| External config (Bitbucket, ortam profilleri) | 🟡 profil yapısı var, external config bağlanmadı |
| OpenShift'e deploy | ❌ erişim yok |

**Bitiş kriteri "pipeline uçtan uca çalışıyor" henüz karşılanmadı** — Faz 0'ın altyapı yarısı
açık. Kod yarısı (iskelet + dikey dilim + parite) tamam.

---

## Teknoloji

- Java 21 (Gradle toolchain otomatik indirir — makinede kurulu olması gerekmez)
- Spring Boot 3.5.6, Spring Cloud 2025.0.0 (OpenFeign)
- Gradle 8.14 (wrapper repoda)
- PostgreSQL / TimescaleDB (JDBC + HikariCP), Redis (Lettuce)

## Modül yapısı

```
path-io-backend-java/
├── appinsight-common/                    ortak: hata tipleri, Redis anahtarları, JSON parite
└── appinsight-admin/
    ├── appinsight-admin-client/          Feign arayüzü + DTO'lar (başka servisler dependency alır)
    └── appinsight-admin-service/         controller → service → repository + Dockerfile + Jenkinsfile
```

Her mini-service `client` + `service` alt projesinden oluşur. `client` yalnızca
arayüz ve DTO içerir; iş mantığı ve repository barındırmaz.

---

## Çalıştırma

Mevcut Node platformunun veritabanını kullanır — **ayrı DB kurmaya gerek yok**,
Node'u kapatmaya da gerek yok. İki sistem aynı DB üzerinde paralel çalışır
(yol haritası §6.2, strangler-fig).

```bash
# 1. Node tarafının docker-compose'u ayakta olmalı (Postgres + Redis)
bash ../start.sh          # ya da zaten çalışıyorsa atla

# 2. Java admin servisi (:8080)
./gradlew :appinsight-admin:appinsight-admin-service:bootRun --args='--spring.profiles.active=local'
```

| Servis | Port |
|--------|------|
| Node backend (mevcut) | 3000 |
| Java admin (yeni) | 8080 |

```bash
./gradlew build     # derleme + test
./gradlew test      # yalnız test
```

---

## Korunan sözleşmeler

Bu projenin tek başarı ölçütü, aşağıdakilerin **değişmemesi**:

1. **REST kontratı** — `/api/v1/` prefix, aynı path, aynı JSON alan adları, aynı status kodları.
   Web portal (React) değişmeden çalışmalı.
2. **WebSocket protokolü** — SDK ↔ backend mesaj tipleri ve alanları (Faz 4).
3. **Redis anahtar & TTL formatları** — `RedisKeys` sınıfında sabitlenmiştir. Paralel
   çalıştırmanın şartı budur; değiştirilirse dedup kaçar, opt-out yeniden tetiklenir.
4. **Veritabanı şeması** — Drizzle migration'ları ile yönetilir. Hibernate `ddl-auto: none`
   ile çalışır ve şemaya asla dokunmaz.

### Doğrulanmış parite (Faz 0)

Canlı Node (`:3000`) ve Java (`:8080`) aynı DB üzerinde karşılaştırıldı — 6 uç,
**bayt bayt aynı yanıt**:

| Uç | Sonuç |
|----|-------|
| `GET /apps` (+ page/limit/search varyantları) | ✅ aynı |
| `GET /apps/{id}` | ✅ aynı |
| `GET /apps/{id}/sdk-config` | ✅ aynı |
| `GET /apps/{bilinmeyen}` → 404 gövdesi | ✅ aynı |
| `POST /apps` → 201, alan kümesi, 64 karakter hex apiKey | ✅ aynı |
| `PATCH /apps/{id}` → 200 | ✅ aynı |
| `DELETE /apps/{id}` → 204, boş gövde, sonrasında 404 | ✅ aynı |

### Bilinen sapmalar

| Konu | Durum |
|------|-------|
| Doğrulama hatası `details` alanı | Node'da Zod `err.errors`, Java'da Bean Validation ihlalleri. `code` ve `message` aynı (`VALIDATION_ERROR` / `Validation failed`), `details` içeriği farklı. Portal bu alanı göstermiyor; contract test'te karşılaştırma dışı. |
| `created_at` / `updated_at` kaynağı | Node'da kolon DEFAULT `now()` (DB saati), Java'da JPA insert öncesi `Instant.now()` (uygulama saati). Ayrı host'ta saat sapması olursa fark oluşur — cutover öncesi doğrulanmalı. |
| Liste sıralaması | İki tarafta da `ORDER BY` yok; sıra Postgres'in fiziksel sırasıdır. Parite korunuyor ama ikisi de deterministik değil. |

**Zaman damgası formatı:** Postgres `timestamptz` mikrosaniye tutar, Node'un `pg`
sürücüsü milisaniyeye kırpar. Java'da `InstantMillisSerializer` aynı kırpmayı yapar ve
kesir hanesini her zaman 3 basamak yazar (`2026-04-22T11:46:28.822Z`). Bu davranış
`InstantMillisSerializerTest` ile korunuyor — kaldırılırsa her tarih alanı sapar.
