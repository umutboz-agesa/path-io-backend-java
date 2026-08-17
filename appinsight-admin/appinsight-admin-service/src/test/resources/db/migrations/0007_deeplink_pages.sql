CREATE TABLE IF NOT EXISTS "deeplink_pages" (
  "id"           uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
  "app_id"       uuid NOT NULL REFERENCES "apps"("id") ON DELETE CASCADE,
  "name"         text NOT NULL,
  "description"  text,
  "page_code"    integer NOT NULL,
  "platform"     text NOT NULL DEFAULT 'ios',
  "param_schema" jsonb NOT NULL DEFAULT '[]'::jsonb,
  "is_active"    boolean NOT NULL DEFAULT true,
  "created_at"   timestamp with time zone NOT NULL DEFAULT now(),
  "updated_at"   timestamp with time zone NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS "deeplink_pages_app_pagecode_platform_uniq"
  ON "deeplink_pages" ("app_id", "page_code", "platform");
