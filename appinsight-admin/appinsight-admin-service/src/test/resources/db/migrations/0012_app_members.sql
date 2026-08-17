-- SDK'dan gelen member_register mesajlarıyla doldurulan UI element kayıt tablosu
CREATE TABLE IF NOT EXISTS "app_members" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
  "app_id" uuid NOT NULL REFERENCES "apps"("id") ON DELETE CASCADE,
  "key" text NOT NULL,
  "label" text,
  "element_type" text DEFAULT 'input' NOT NULL,
  "screen" text NOT NULL,
  "platform" text DEFAULT 'ios' NOT NULL,
  "last_registered_at" timestamp with time zone DEFAULT now() NOT NULL,
  "created_at" timestamp with time zone DEFAULT now() NOT NULL,
  CONSTRAINT "app_members_app_key_platform_uniq" UNIQUE ("app_id", "key", "platform")
);
