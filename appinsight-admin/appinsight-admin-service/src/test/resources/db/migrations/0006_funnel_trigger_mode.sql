ALTER TABLE funnels
  ADD COLUMN IF NOT EXISTS trigger_mode text NOT NULL DEFAULT 'session_once';
