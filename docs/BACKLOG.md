# Backlog — bekleyen işler

Fazlar ilerlerken kenara konan, unutulmaması gereken maddeler.

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
