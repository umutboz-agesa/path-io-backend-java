-- Drop old unique index
DROP INDEX IF EXISTS screens_app_name_uniq;

-- Add platform & canonical tracking columns
ALTER TABLE screens
  ADD COLUMN IF NOT EXISTS platform       text        NOT NULL DEFAULT '',
  ADD COLUMN IF NOT EXISTS canonical_name text,
  ADD COLUMN IF NOT EXISTS first_seen_at  timestamptz NOT NULL DEFAULT now(),
  ADD COLUMN IF NOT EXISTS last_seen_at   timestamptz NOT NULL DEFAULT now(),
  ADD COLUMN IF NOT EXISTS event_count    integer     NOT NULL DEFAULT 0;

-- New unique index includes platform so same screen name can exist on ios + android
CREATE UNIQUE INDEX IF NOT EXISTS screens_app_name_platform_uniq
  ON screens(app_id, name, platform);
