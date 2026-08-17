-- insight target_screen (text) → target_screens (jsonb array)
ALTER TABLE "insights" ADD COLUMN "target_screens" jsonb NOT NULL DEFAULT '[]'::jsonb;

UPDATE "insights"
SET "target_screens" = CASE
  WHEN "target_screen" IS NOT NULL AND "target_screen" != ''
  THEN jsonb_build_array("target_screen")
  ELSE '[]'::jsonb
END;

ALTER TABLE "insights" DROP COLUMN "target_screen";
