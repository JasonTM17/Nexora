-- M2-DB01 identity profile relation and subject-scoped RLS.
--
-- The external identity subject is stored only as the verified `sub` UUID. No
-- JWT role, user_metadata, app_metadata, or provider-owned auth relation is an
-- authorization source in this schema.

BEGIN;
SET LOCAL ROLE nexora_migrator;

CREATE TABLE nexora.profiles (
  subject_id uuid PRIMARY KEY,
  display_name text NOT NULL
    CHECK (char_length(display_name) BETWEEN 1 AND 120),
  locale text NOT NULL
    CHECK (locale ~ '^[A-Za-z]{2,3}(?:-[A-Za-z0-9]{2,8})*$'),
  reduced_motion boolean NOT NULL DEFAULT false,
  high_contrast boolean NOT NULL DEFAULT false,
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  created_at timestamp with time zone NOT NULL DEFAULT transaction_timestamp(),
  updated_at timestamp with time zone NOT NULL DEFAULT transaction_timestamp(),
  CONSTRAINT profiles_updated_after_created
    CHECK (updated_at >= created_at)
);

COMMENT ON TABLE nexora.profiles IS
  'Allowlisted user-editable profile data keyed by verified identity subject; never an authorization input.';

CREATE FUNCTION nexora.advance_row_version()
RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = pg_catalog
AS $function$
BEGIN
  NEW.version := OLD.version + 1;
  NEW.updated_at := transaction_timestamp();
  RETURN NEW;
END
$function$;

REVOKE ALL ON FUNCTION nexora.advance_row_version() FROM PUBLIC;

CREATE TRIGGER profiles_advance_version
BEFORE UPDATE ON nexora.profiles
FOR EACH ROW
EXECUTE FUNCTION nexora.advance_row_version();

ALTER TABLE nexora.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE nexora.profiles FORCE ROW LEVEL SECURITY;

CREATE POLICY profiles_select_own_subject
ON nexora.profiles
FOR SELECT
TO nexora_runtime
USING (
  subject_id = NULLIF(current_setting('nexora.subject_id', true), '')::uuid
);

CREATE POLICY profiles_insert_own_subject
ON nexora.profiles
FOR INSERT
TO nexora_runtime
WITH CHECK (
  subject_id = NULLIF(current_setting('nexora.subject_id', true), '')::uuid
);

CREATE POLICY profiles_update_own_subject
ON nexora.profiles
FOR UPDATE
TO nexora_runtime
USING (
  subject_id = NULLIF(current_setting('nexora.subject_id', true), '')::uuid
)
WITH CHECK (
  subject_id = NULLIF(current_setting('nexora.subject_id', true), '')::uuid
);

GRANT SELECT ON nexora.profiles TO nexora_runtime;
GRANT INSERT (
  subject_id,
  display_name,
  locale,
  reduced_motion,
  high_contrast
) ON nexora.profiles TO nexora_runtime;
GRANT UPDATE (
  display_name,
  locale,
  reduced_motion,
  high_contrast
) ON nexora.profiles TO nexora_runtime;

COMMIT;
