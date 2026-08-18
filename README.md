# Path.IO Backend — Java / Spring Boot

AppInsight platformunun Node.js/TypeScript backend'inin kurumsal Java standardına
taşınmış hâli. Hedef: **davranışsal olarak birebir aynı** sistem.

Yol haritası: `PathIO_Java_Donusum_Yol_Haritasi.docx` (ana repo kökü).

---

## Durum

| Faz | Kapsam | Durum |
|-----|--------|-------|
| 0 | Gradle iskelet, common lib, admin dikey dilimi (apps CRUD), Dockerfile + Jenkinsfile | 🟢 Kod + altyapı tamam, deploy hattı bekliyor |
| 1 | admin mini-service — 51 endpoint | 🟢 **48/51** — kalan 3 uç WS'e bağlı, Faz 4'te |
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
| **13 tablonun JPA entity + jsonb mapping'i** | ✅ **13/13**, gerçek şemaya karşı test edildi |
| PostgreSQL bağlantısı | ✅ |
| Redis bağlantısı | ✅ yaz/oku testiyle doğrulandı |
| TimescaleDB ayrı pool | ✅ ayrı Hikari havuzu + hypertable sorgusu doğrulandı |
| Dockerfile | ✅ image build edildi, container ayağa kalktı, **yanıtı Node ile bayt bayt aynı** |
| Jenkinsfile | ⚠️ yazıldı, **çalıştırılmadı** — Jenkins erişimi gerekiyor |
| External config (Bitbucket) | 🟡 profil yapısı var, external config bağlanmadı |
| OpenShift'e deploy | ❌ erişim gerekiyor |

Kalan üç madde de **altyapı erişimi** istiyor (Jenkins, Bitbucket external config, OpenShift) —
kod tarafında Faz 0'da yapılacak bir şey kalmadı.

### Entity eşleme doğrulaması

13 entity'nin tamamı **gerçek Drizzle şemasına** karşı test ediliyor — Hibernate'in kendi
ürettiği şemaya değil. İki varyant aynı testleri koşturur:

| Test | Şema kaynağı | Ne zaman koşar |
|------|--------------|----------------|
| `EntityMappingIT` | Testcontainers + gerçek migration'lar (sıfırdan) | Docker varsa (CI) |
| `LocalDbEntityMappingTest` | Geliştiricinin canlı veritabanı | Postgres 5432'de açıksa |

İkisi de yoksa testler **atlanır**, kırmızı yanmaz. Testler transaction içinde koşar ve geri
alınır — canlı veriye kalıcı satır yazılmaz.

Migration'lar `src/test/resources/db/migrations` altında kopya tutulur;
`scripts/sync-migrations.sh` ile tazelenir. **Node tarafında yeni migration eklenip bu script
çalıştırılmazsa test eski şemaya karşı geçmeye devam eder** — bilinçli kabul edilmiş risk.

### Eşlemede yakalanan üç tuzak

| Tablo | Tuzak |
|-------|-------|
| `payload_templates` | Kolon adı `schema` — PostgreSQL rezerve kelimesi, tırnaklanması şart (`@Column(name = "\"schema\"")`). |
| `insights` | `target_screens` **jsonb dizi**, `text[]` değil. `apps.platforms` ile aynı sanılırsa okuma anında patlar. |
| `sessions` | Birincil anahtar `text`, `uuid` değil — değeri SDK üretir, olduğu gibi saklanır. |

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

# Docker image
docker build -f appinsight-admin/appinsight-admin-service/Dockerfile -t appinsight-admin:local .

# Node tarafında migration eklendiyse test fixture'larını tazele
./scripts/sync-migrations.sh
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

### Doğrulanmış parite

Canlı Node (`:3000`) ve Java (`:8080`) aynı DB üzerinde karşılaştırıldı.

**apps (6 uç)** — hepsi bayt bayt aynı:

| Uç | Sonuç |
|----|-------|
| `GET /apps` (+ page/limit/search varyantları) | ✅ aynı |
| `GET /apps/{id}` · `sdk-config` · 404 gövdesi | ✅ aynı |
| `POST /apps` → 201, alan kümesi, 64 karakter hex apiKey | ✅ aynı |
| `PATCH /apps/{id}` → 200 · `DELETE` → 204 + sonrasında 404 | ✅ aynı |

**screens (3) · payload-templates (5) · deeplink-pages (4)**:

| Uç | Sonuç |
|----|-------|
| `GET /apps/{id}/screens` — düz liste + kanonik gruplama | ✅ aynı |
| `PATCH /apps/{id}/screens/{sid}` → `{"ok":true}`, 4 doğrulama senaryosu | ✅ aynı |
| `DELETE /apps/{id}/screens/{sid}` → 204, kayıt yoksa da 404 atmaz | ✅ aynı |
| payload-templates CRUD (5 uç) + `App not found` / `Template not found` | ✅ aynı |
| deeplink-pages CRUD (4 uç) + Zod default'ları (`ios`, `[]`, `true`) | ✅ aynı |
| Tanımsız route → Fastify'ın 404 gövdesi (alan sırası dahil) | ✅ aynı |

**devices (5) · gcl-queries (6) · members (1)**:

| Uç | Sonuç |
|----|-------|
| `GET /apps/{id}/activity` — cihaz başına son event (`DISTINCT ON`) | ✅ aynı |
| `GET /apps/{id}/activity?deviceId=&limit=&offset=` | ✅ aynı |
| `GET /apps/{id}/devices` · `sessions` · `devices/{d}/sessions` | ✅ aynı |
| `GET /apps/{id}/sessions/{sid}` — 12 oturum × 2 liste, sıra dahil | ✅ aynı |
| gcl-queries CRUD + hits (6 uç, ham dizi + PUT + farklı 404 gövdesi) | ✅ aynı |
| `GET /apps/{id}/members` (+ `?screen=`) | ✅ aynı |

**integrations (5)**:

| Uç | Sonuç |
|----|-------|
| `GET` liste — ham dizi, credentials maskeli (alan sırası dahil) | ✅ aynı |
| `POST` → 201 · aynı tip ikinci kez → 409 | ✅ aynı |
| `PUT` (kısmi güncelleme, `status`→pending, `lastError`→null) · `DELETE` → 204 | ✅ aynı |
| Doğrulama: 8 senaryo (eksik/hatalı config ve credentials, tip enum) | ✅ aynı |
| `POST /test` — eksik alan dalları, `status`/`lastError` yazımı | ✅ aynı |

**funnels (7/8) · insights (6/8)**:

| Uç | Sonuç |
|----|-------|
| funnels CRUD + toggle + doğrulama (isim, step sayısı, timeout aralığı) | ✅ aynı |
| `GET /funnels/{id}/history` (+ limit/offset/status/userAction/deviceId filtreleri) | ✅ aynı |
| `GET /funnels/{id}/history/devices` — ham SQL, camelCase alias'lar | ✅ aynı |
| insights CRUD + deliveries + `Cannot edit a sent insight` | ✅ aynı |
| Insight doğrulama: status/display.style/action.type/target union — 14 senaryo | ✅ aynı |

**Funnel yanıtlarında iki farklı şekil:** `GET liste` motor temsilini (`FunnelDefinition`),
`POST`/`PATCH` ham Drizzle satırını döner. Alanlar aynı, **sıra farklı** (`triggerMode` ↔
`isActive`). Node'da liste `toDefinition()`'dan, create/update doğrudan satırdan geçtiği için
oluşan bir tutarsızlık; iki ayrı DTO ile korunuyor.

**Kısmi güncellemede "gönderilmedi ≠ null":** funnels ve insights PATCH gövdeleri ham `Map`
olarak alınıyor. Node her alanı `!== undefined` ile kontrol ettiğinden gönderilmeyen alan
korunur, açıkça `null` gönderilen alan temizlenir — record ile bu ikisi ayırt edilemezdi
(`startsAt`/`expiresAt`/`scheduledAt` için kritik).

**Credential maskeleme:** `private_key` → `••••••••`, `private_key_id` → ilk 8 karakter +
`••••••••`. Node bunu destructuring ile yaptığı için maskelenen iki alan **nesnenin sonuna
taşınıyor**; alan sırası JSON'da göründüğü için bu davranış da birebir üretildi.
Ham servis hesabı anahtarı hiçbir uçtan dışarı çıkmıyor.

#### Zaman serisi uçlarında üç tuzak

Bu uçlar Node'da drizzle'ı atlayıp ham `pg` sorgusu kullanıyor; bu üç davranış oradan geliyor:

1. **Zaman damgası ISO değil.** drizzle-orm, `pg` sürücüsünün timestamptz ayrıştırıcısını global
   olarak değiştiriyor; ham sorgu sonucu Postgres'in kendi metni oluyor:
   `2026-04-25 20:29:42.499+00` — `T`/`Z` yok, sondaki sıfırlar kırpılmış. Java'da aynı çıktı
   için render Postgres'e bırakıldı (`::text`) ve **oturum saat dilimi UTC'ye sabitlendi**
   (`connection-init-sql`), yoksa PgJDBC JVM saat dilimini kullanıp `+03` yazardı.
2. **`COUNT`/`SUM` sonuçları string.** `pg` sürücüsü `bigint` değerlerini JavaScript'te string
   döndürür (güvenli tamsayı sınırı). Yani `"views":"3"`, `3` değil.
3. **Alan adları snake_case.** Ham SQL sonucu olduğu gibi JSON'a çevriliyor; `device_id`,
   `screen_name`… Tipli DTO'ya çevirmek portalin okuduğu adları kaydırırdı.

`ORDER BY` da `::text` alias'ına değil, gerçek `timestamptz` kolonuna bakmalı — alias üzerinden
sıralamak aynı saniyedeki satırların sırasını değiştiriyordu.

### Bilinen sapmalar

| Konu | Durum |
|------|-------|
| Doğrulama hatası `details` alanı | Node'da Zod `err.errors`, Java'da Bean Validation ihlalleri. `code` ve `message` aynı (`VALIDATION_ERROR` / `Validation failed`), `details` içeriği farklı. Portal bu alanı göstermiyor; contract test'te karşılaştırma dışı. |
| `created_at` / `updated_at` kaynağı | Node'da kolon DEFAULT `now()` (DB saati), Java'da JPA insert öncesi `Instant.now()` (uygulama saati). Ayrı host'ta saat sapması olursa fark oluşur — cutover öncesi doğrulanmalı. |
| Liste sıralaması (`/apps`) | İki tarafta da `ORDER BY` yok; sıra Postgres'in fiziksel sırasıdır. Parite korunuyor ama ikisi de deterministik değil. |
| `POST /integrations/{id}/test` hata **metni** | Yapı aynı (`{"ok":false,"error":…}`), `status`/`lastError` yazımı aynı; ama metnin kendisi istemci kütüphanesinden geliyor. Node'un `@google-cloud/pubsub`'ı 60 sn yeniden deneyip uzun bir gRPC yığını döndürüyor, Java istemcisi hemen `Invalid PKCS#8 data.` diyor. Aynı cümleyi üretmek mümkün değil. |
| Credential doğrulamada e-posta/URL uç örnekleri | Zod'un `.email()` / `.url()` regex'leri ile Java'daki basit kontroller tuhaf ama teknik olarak geçerli adreslerde ayrışabilir. Gerçek servis hesabı anahtarlarında fark üretmez. |

### Bilerek taşınan hata

`GET /apps/{id}/screens` yanıtındaki grup `lastSeenAt` değeri **yanlış** — ve Java tarafında
da yanlış üretiliyor. Node'daki ifade `members.map(m => m.lastSeenAt).sort().at(-1)`;
JavaScript'te karşılaştırıcısız `sort()` elemanları string'e çevirir, `Date` de
`"Mon Aug 17 2026 …"` biçimine dönüşür. Sonuç: en yeni tarih değil, **haftanın gün adı
alfabetik olarak en sonda olan** seçilir ("Fri" &lt; "Mon" &lt; "Sat" &lt; "Sun" &lt; "Thu").

Düzeltilmedi çünkü bu proje davranışsal parite üzerine kurulu: burada düzeltmek gölge trafik
karşılaştırmasında sürekli fark üretir ve gerçek regresyonları gizler.
`ScreenServiceGroupingTest` bu davranışı kilitliyor; `docs/BACKLOG.md`'de cutover sonrası
İKİ sistemde birlikte düzeltilmek üzere kayıtlı.

**Zaman damgası formatı:** Postgres `timestamptz` mikrosaniye tutar, Node'un `pg`
sürücüsü milisaniyeye kırpar. Java'da `InstantMillisSerializer` aynı kırpmayı yapar ve
kesir hanesini her zaman 3 basamak yazar (`2026-04-22T11:46:28.822Z`). Bu davranış
`InstantMillisSerializerTest` ile korunuyor — kaldırılırsa her tarih alanı sapar.
