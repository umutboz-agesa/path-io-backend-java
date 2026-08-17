-- Faz 6: Insight Engine migrations

-- funnels: globalTimeoutMs + targetFilter
ALTER TABLE "funnels"
  ADD COLUMN IF NOT EXISTS "global_timeout_ms" integer DEFAULT 1800000 NOT NULL,
  ADD COLUMN IF NOT EXISTS "target_filter" jsonb DEFAULT '{}'::jsonb NOT NULL;

-- insights: frequency
ALTER TABLE "insights"
  ADD COLUMN IF NOT EXISTS "frequency" jsonb DEFAULT '{"max_per_device":1,"window_hours":0}'::jsonb NOT NULL;

-- insight_deliveries
CREATE TABLE IF NOT EXISTS "insight_deliveries" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
  "insight_id" uuid NOT NULL REFERENCES "insights"("id") ON DELETE CASCADE,
  "app_id" uuid NOT NULL,
  "device_id" text NOT NULL,
  "status" text DEFAULT 'delivered' NOT NULL,
  "funnel_id" uuid REFERENCES "funnels"("id") ON DELETE SET NULL,
  "trigger_ctx" jsonb DEFAULT '{}'::jsonb NOT NULL,
  "delivered_at" timestamp with time zone DEFAULT now() NOT NULL
);

CREATE INDEX IF NOT EXISTS "idx_deliveries_insight_device" ON "insight_deliveries" ("insight_id", "device_id");
CREATE INDEX IF NOT EXISTS "idx_deliveries_device_date" ON "insight_deliveries" ("device_id", "delivered_at" DESC);
