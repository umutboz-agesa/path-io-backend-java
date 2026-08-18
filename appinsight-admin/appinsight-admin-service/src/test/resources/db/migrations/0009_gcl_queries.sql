CREATE TABLE IF NOT EXISTS gcl_queries (
  id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  app_id      UUID        NOT NULL REFERENCES apps(id) ON DELETE CASCADE,
  name        TEXT        NOT NULL,
  description TEXT,
  expression  TEXT        NOT NULL,
  is_active   BOOLEAN     NOT NULL DEFAULT true,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS gcl_query_hits (
  id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  query_id    UUID        NOT NULL REFERENCES gcl_queries(id) ON DELETE CASCADE,
  app_id      UUID        NOT NULL,
  device_id   TEXT        NOT NULL,
  ts          TIMESTAMPTZ NOT NULL,
  event_data  JSONB       NOT NULL DEFAULT '{}',
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS gcl_query_hits_query_ts_idx ON gcl_query_hits(query_id, ts DESC);
CREATE INDEX IF NOT EXISTS gcl_query_hits_app_ts_idx   ON gcl_query_hits(app_id,   ts DESC);
