# Backlog — bekleyen işler

Fazlar ilerlerken kenara konan, unutulmaması gereken maddeler.

---

## Faz 1'den kalanlar — WebSocket bekliyor (Faz 4)

Admin servisinde **48/51** uç yazıldı. Kalan üçü de bağlı SDK'lara doğrudan WS mesajı
gönderdiği için realtime mini-service'i olmadan yazılamaz.

| # | Uç | Neden bekliyor |
|---|-----|----------------|
| 1 | `POST /apps/:appId/funnels/:id/restart` | Redis state/dedup/opt-out anahtarlarını temizler **ve bağlı cihazlara `force_clear_optout` gönderir**. Yalnız Redis kısmını yapmak, cihazlardaki yerel opt-out'ların takılı kalmasına yol açardı — insight bir daha hiç gösterilmezdi. |
| 2 | `POST /apps/:appId/insights/:id/send` | Operatörün elle push'u; `insightEngine.sendManual` doğrudan WS'e yazıyor. |
| 3 | `POST /apps/:appId/data-push` | `pushService.sendDataPush` — insight kaydetmeden anlık WS data push. |

Faz 4'te realtime servisi ayağa kalkınca bu üçü admin'den mi çağrılacak (Feign → realtime)
yoksa realtime'a mı taşınacak, §3.4'teki dispatch kararıyla birlikte netleşecek.

---

## Faz 0'dan kalanlar (altyapı erişimi bekliyor)

Kod tarafında yapılacak bir şey yok; üçü de dış sistem erişimi istiyor.

| # | Madde | Ne gerekiyor | Not |
|---|-------|--------------|-----|
| 1 | Jenkins pipeline'ını gerçekten çalıştır | Jenkins erişimi + `REGISTRY` credential'ı | `Jenkinsfile` yazıldı ama hiç koşmadı. İlk koşuda `junit` adımının test XML yolunu bulduğu doğrulanmalı. |
| 2 | Bitbucket external config bağlantısı | Bitbucket repo + Spring Cloud Config veya dosya montajı | Şu an yalnız `application-{profile}.yml` var. DB/Redis/GCP kimlikleri buradan gelecek. |
| 3 | OpenShift'e deploy | `oc` erişimi + `maf-applications` reposunda Deployment/Service/Route/ConfigMap | Faz 0'ın "pipeline uçtan uca çalışıyor" bitiş kriteri buna bağlı. |

**Neden bekliyor:** üçü de kurumsal altyapıya erişim gerektiriyor, yerelde taklit edilemez.
Docker image'ın çalıştığı yerelde kanıtlandı (container ayağa kalktı, yanıtı Node ile bayt bayt
aynı) — yani deploy hattının kod tarafı hazır.

---

## 🔴 Şema kayması — migration'lar gerçek şemayı üretmiyor

**Faz 2'de kesişti** (eventProcessor `screen_events`'e `session_id` yazıyor) ve incelenince
sorun tek kolondan çok daha büyük çıktı.

### Bulgu 1 — Drizzle journal'ı 13 migration'ın yalnız 5'ini izliyor

`migrate.ts` drizzle'ın migrator'ını kullanıyor; migrator **`meta/_journal.json`'daki kayıtları**
uygular, klasördeki .sql dosyalarını değil.

| | Sayı |
|---|---|
| Klasördeki .sql dosyası | 13 |
| Journal'da kayıtlı | 5 |
| Veritabanına uygulanmış (`drizzle.__drizzle_migrations`) | 4 |

İzlenmeyen 8 migration: `0004_funnel_expires_at`, `0005_history_tracking`,
`0006_funnel_trigger_mode`, `0007_deeplink_pages`, `0009_gcl_queries`,
`0010_insight_target_screens`, `0011_insight_gcl_data_step`, `0012_app_members`.

**Sonuç:** sıfır bir veritabanında `npm run db:migrate` çalıştırıldığında ortaya çıkan şemada
`funnels.expires_at`/`starts_at`/`trigger_mode`, `deeplink_pages`, `gcl_queries`,
`gcl_query_hits`, `app_members`, `insights.target_screens`, `insights.gcl_data_step`
**bulunmaz**. Yani test/prod ilk kurulumu bozuk şema üretir.

### Bulgu 2 — Hiçbir dosyada olmayan nesneler

`screen_events.session_id` kolonu ve `idx_screen_events_session` / `idx_screen_events_screen`
index'leri canlı DB'de var ama **hiçbir .sql dosyasında yok** — tamamen elle eklenmişler.
`activity` ve `sessions/:id` uçları bu kolona bağlı.

### Neden şimdiye kadar fark edilmedi

Geliştirici makinesindeki veritabanı elle tamamlanmış. Java tarafındaki `EntityMappingIT` de
sorunu maskeliyordu: journal'ı atlayıp 13 .sql dosyasını **doğrudan** çalıştırıyor, yani
gerçek migration yolundan daha eksiksiz bir şema kuruyor.

### Önerilen düzeltme (Node repo'su — ONAY BEKLİYOR)

1. Eksik 8 migration için journal kayıtlarını ekle **veya** hepsini kapsayan idempotent tek
   bir baseline migration yaz.
2. Dosyası hiç olmayan nesneler için:

```sql
-- 0013_screen_events_session_id.sql
ALTER TABLE screen_events ADD COLUMN IF NOT EXISTS session_id text;
CREATE INDEX IF NOT EXISTS idx_screen_events_session ON screen_events (session_id, "time" DESC);
CREATE INDEX IF NOT EXISTS idx_screen_events_screen  ON screen_events (app_id, screen_name, "time" DESC);
```

3. Doğrulama: boş bir veritabanında `db:migrate` çalıştırıp şemayı canlı DB ile karşılaştır.

Mevcut ortamlarda hiçbir şey değişmez (nesneler zaten var). Node backend'ine dokunmama kararı
gereği **uygulanmadı**.

---

## Teknik borç / riskler

| # | Konu | Risk | Karar |
|---|------|------|-------|
| 1 | Migration kopyası test resource'larında | Node'da yeni migration eklenip `scripts/sync-migrations.sh` çalıştırılmazsa entity testi ESKİ şemaya karşı geçer, kayma fark edilmez | Kabul edildi. Alternatifi Java testinin Node repo'suna dosya yolu bağımlılığıydı; Jenkins'te kırılırdı. |
| 2 | `created_at` / `updated_at` kaynağı | Node'da DB `now()`, Java'da uygulama saati. Ayrı host'ta saat sapması olursa değerler ayrışır | Cutover öncesi doğrulanmalı. Gerekirse `insertable=false` + DB default'a bırakılır. |
| 3 | Doğrulama hatası `details` alanı | Zod ile Bean Validation farklı şekil üretiyor | Kabul edildi. `code`/`message` aynı; portal `details`'i göstermiyor. Contract test'te karşılaştırma dışı. |
| 4 | Liste sıralaması | İki tarafta da `ORDER BY` yok; sıra Postgres'in fiziksel sırası | Parite korunuyor ama ikisi de deterministik değil. Cutover sonrası düzeltilebilir. |
| 5 | `integrations.credentials` | GCP servis hesabı anahtarı tutuyor | REST yanıtında ASLA dönülmemeli. Faz 1'de integrations uçları yazılırken DTO'ya kopyalanmadığı test edilmeli. |

---

## Cutover sonrası düzeltilecek — İKİ sistemde birlikte

| # | Konu | Detay |
|---|------|-------|
| 1 | **Ekran grubu `lastSeenAt` yanlış** | `screens.ts`: `members.map(m => m.lastSeenAt).sort().at(-1)` — JS'te karşılaştırıcısız `sort()` `Date`'leri string'e çevirir, sıralama haftanın gün adına göre alfabetik olur ("Fri" < "Mon" < "Sat" < "Sun" < "Thu"). En yeni tarih değil, gün adı alfabetik son olan seçilir. **Portal yanlış tarih gösteriyor.** Java tarafında bilerek birebir kopyalandı (parite), `ScreenServiceGroupingTest` kilitliyor. Düzeltme: `Math.max` / `Comparator.naturalOrder()` — aynı anda iki tarafta, sonra test güncellenir. |

**Neden şimdi değil:** gölge trafik karşılaştırmasında Java'yı "düzeltmek" sürekli fark üretir
ve gerçek regresyonları gizler. Parite kanıtlandıktan sonra iki sistemde birlikte düzeltilir.

---

## Yol haritası dokümanında düzeltilmesi gerekenler

`PathIO_Java_Donusum_Yol_Haritasi.docx` incelemesinde çıkanlar:

| # | Bölüm | Düzeltme |
|---|-------|----------|
| 1 | §2.2 başlığı | "54 endpoint" yazıyor, tablo toplamı ve gerçek sayım **51**. |
| 2 | §2 envanter | "12 PostgreSQL tablosu" — gerçek sayı **13** (`gcl_query_hits` sayılmamış). |
| 3 | §6.1 sözleşme listesi | SDK→backend'de `member_register`, backend→SDK'da `init_error` / `config_update` / `pending_insights` eksik. |
| 4 | §6.1 | Portal WS sözleşmesi (`live_event`, `devices`, `subscribed`, `filter`…) hiç sayılmamış — korunacak sözleşme üç değil **dört**. |
| 5 | Kapsam satırı | "iOS SDK v1.2.13" — güncel sürüm **v1.5.0**; korunacak yüzey büyüdü (`url` / `return_to` / `set_value` aksiyonları, member registry). |
