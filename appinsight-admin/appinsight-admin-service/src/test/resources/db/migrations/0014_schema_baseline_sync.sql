-- Şema baseline senkronizasyonu
--
-- NEDEN: Canlı veritabanındaki bazı nesneler hiçbir migration dosyasında yoktu — büyük
-- olasılıkla `drizzle-kit push` ile ya da elle uygulanmışlar. Sonuç: migration'lardan
-- sıfırdan kurulan bir ortam (test/prod ilk kurulum, CI) EKSİK şema üretiyordu:
--   • sessions ve payload_templates tabloları hiç oluşmuyordu
--   • apps.bundle_ids (jsonb) yerine eski apps.bundle_id (text) kalıyordu
--   • insights.display / data / template_id / scheduled_at yoktu
--   • insight_deliveries.action_clicked_at yoktu
--
-- Bu dosya farkı kapatır. TAMAMI IDEMPOTENT: nesneler zaten varsa hiçbir şey değişmez.

-- ── sessions ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS "sessions" (
  "id"          text PRIMARY KEY NOT NULL,
  "app_id"      uuid NOT NULL REFERENCES "apps"("id") ON DELETE CASCADE,
  "device_id"   text NOT NULL,
  "platform"    text NOT NULL DEFAULT '',
  "app_version" text NOT NULL DEFAULT '',
  "os_version"  text NOT NULL DEFAULT '',
  "model"       text NOT NULL DEFAULT '',
  "started_at"  timestamptz NOT NULL DEFAULT now(),
  "ended_at"    timestamptz
);

-- ── payload_templates ───────────────────────────────────────────────────────
-- Kolon adı "schema" — PostgreSQL rezerve kelimesi, tırnaklı kullanılmalı.
CREATE TABLE IF NOT EXISTS "payload_templates" (
  "id"           uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
  "app_id"       uuid NOT NULL REFERENCES "apps"("id") ON DELETE CASCADE,
  "name"         text NOT NULL,
  "description"  text,
  "schema"       jsonb NOT NULL DEFAULT '{}'::jsonb,
  "default_data" jsonb NOT NULL DEFAULT '{}'::jsonb,
  "platforms"    text[] NOT NULL DEFAULT '{}'::text[],
  "created_at"   timestamptz NOT NULL DEFAULT now(),
  "updated_at"   timestamptz NOT NULL DEFAULT now()
);

-- ── apps.bundle_id (text) → bundle_ids (jsonb) ──────────────────────────────
ALTER TABLE "apps" ADD COLUMN IF NOT EXISTS "bundle_ids" jsonb NOT NULL DEFAULT '{}'::jsonb;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'apps' AND column_name = 'bundle_id'
  ) THEN
    -- Eski tek değerli kolon iOS bundle'ı olarak taşınır.
    EXECUTE $mig$
      UPDATE "apps"
      SET "bundle_ids" = jsonb_build_object('ios', "bundle_id")
      WHERE "bundle_id" IS NOT NULL AND "bundle_id" != ''
    $mig$;
    EXECUTE 'ALTER TABLE "apps" DROP COLUMN "bundle_id"';
  END IF;
END $$;

-- ── insights eksik kolonları ────────────────────────────────────────────────
ALTER TABLE "insights" ADD COLUMN IF NOT EXISTS "display"      jsonb NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE "insights" ADD COLUMN IF NOT EXISTS "data"         jsonb NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE "insights" ADD COLUMN IF NOT EXISTS "template_id"  uuid REFERENCES "payload_templates"("id") ON DELETE SET NULL;
ALTER TABLE "insights" ADD COLUMN IF NOT EXISTS "scheduled_at" timestamptz;

-- ── insight_deliveries eksik kolonu ─────────────────────────────────────────
ALTER TABLE "insight_deliveries" ADD COLUMN IF NOT EXISTS "action_clicked_at" timestamptz;

-- ── sessions index'leri ─────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_sessions_app    ON sessions (app_id, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_sessions_device ON sessions (app_id, device_id, started_at DESC);
