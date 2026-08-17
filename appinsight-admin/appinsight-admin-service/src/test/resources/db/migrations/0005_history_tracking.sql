-- Add starts_at to funnels (funnel activation window start date)
ALTER TABLE funnels ADD COLUMN IF NOT EXISTS starts_at timestamp with time zone;

-- Add user interaction tracking to insight_deliveries
ALTER TABLE insight_deliveries ADD COLUMN IF NOT EXISTS user_action text;
ALTER TABLE insight_deliveries ADD COLUMN IF NOT EXISTS interacted_at timestamp with time zone;

-- Index for funnel history queries
CREATE INDEX IF NOT EXISTS idx_deliveries_funnel_date ON insight_deliveries (funnel_id, delivered_at DESC);
