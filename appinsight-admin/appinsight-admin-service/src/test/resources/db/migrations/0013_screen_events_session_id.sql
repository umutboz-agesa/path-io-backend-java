-- screen_events.session_id + eksik index'ler
--
-- Bu nesneler canlı veritabanına ELLE eklenmiş ve hiçbir migration dosyasında yoktu.
-- activity ve sessions/:id uçları session_id'ye bağlı; migration'lardan sıfırdan kurulan
-- bir ortamda (test/prod ilk kurulum, CI) kolon oluşmadığı için bu uçlar 500 veriyordu.
--
-- Tamamı idempotent: mevcut ortamlarda hiçbir şey değişmez.
ALTER TABLE screen_events ADD COLUMN IF NOT EXISTS session_id text;

CREATE INDEX IF NOT EXISTS idx_screen_events_session ON screen_events (session_id, "time" DESC);
CREATE INDEX IF NOT EXISTS idx_screen_events_screen  ON screen_events (app_id, screen_name, "time" DESC);
