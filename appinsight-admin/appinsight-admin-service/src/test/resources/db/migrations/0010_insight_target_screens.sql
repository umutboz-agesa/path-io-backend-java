-- insight target_screen (text) → target_screens (jsonb array)
--
-- IDEMPOTENT: bu migration journal'a sonradan eklendi ve zaten uygulanmış
-- veritabanlarında yeniden çalışacak. Dönüşüm yalnızca eski kolon hâlâ duruyorsa yapılır.
ALTER TABLE "insights" ADD COLUMN IF NOT EXISTS "target_screens" jsonb NOT NULL DEFAULT '[]'::jsonb;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'insights' AND column_name = 'target_screen'
  ) THEN
    EXECUTE $mig$
      UPDATE "insights"
      SET "target_screens" = CASE
        WHEN "target_screen" IS NOT NULL AND "target_screen" != ''
        THEN jsonb_build_array("target_screen")
        ELSE '[]'::jsonb
      END
    $mig$;
    EXECUTE 'ALTER TABLE "insights" DROP COLUMN "target_screen"';
  END IF;
END $$;
