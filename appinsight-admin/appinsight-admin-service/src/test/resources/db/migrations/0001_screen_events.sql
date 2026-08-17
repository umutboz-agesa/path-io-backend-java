CREATE TABLE IF NOT EXISTS screen_events (
  time          TIMESTAMPTZ        NOT NULL,
  app_id        UUID               NOT NULL,
  device_id     TEXT               NOT NULL,
  screen_name   TEXT               NOT NULL,
  event_type    TEXT               NOT NULL DEFAULT 'screen_view',
  duration_ms   INTEGER,
  platform      TEXT,
  os_version    TEXT,
  app_version   TEXT,
  properties    JSONB              DEFAULT '{}'
);

SELECT create_hypertable('screen_events', 'time', if_not_exists => TRUE);

CREATE INDEX IF NOT EXISTS idx_screen_events_app_time
  ON screen_events (app_id, time DESC);

CREATE INDEX IF NOT EXISTS idx_screen_events_device
  ON screen_events (device_id, time DESC);

CREATE INDEX IF NOT EXISTS idx_screen_events_screen
  ON screen_events (app_id, screen_name, time DESC);
