CREATE TABLE IF NOT EXISTS "integrations" (
  "id"           uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
  "app_id"       uuid NOT NULL REFERENCES "apps"("id") ON DELETE CASCADE,
  "type"         text NOT NULL DEFAULT 'gcl',
  "config"       jsonb NOT NULL DEFAULT '{}'::jsonb,
  "credentials"  jsonb NOT NULL DEFAULT '{}'::jsonb,
  "is_active"    boolean NOT NULL DEFAULT false,
  "status"       text NOT NULL DEFAULT 'pending',
  "last_error"   text,
  "created_at"   timestamp with time zone NOT NULL DEFAULT now(),
  "updated_at"   timestamp with time zone NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS "integrations_app_type_uniq"
  ON "integrations" ("app_id", "type");
