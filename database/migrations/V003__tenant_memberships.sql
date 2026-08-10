-- M2-DB01 tenant roots, membership authority, and forced tenant RLS.

BEGIN;
SET LOCAL ROLE nexora_migrator;

CREATE TYPE nexora.membership_status AS ENUM (
  'INVITED',
  'ACTIVE',
  'SUSPENDED',
  'REMOVED'
);

CREATE TYPE nexora.tenant_role AS ENUM (
  'OWNER',
  'ADMIN',
  'EDITOR',
  'CONTENT_CREATOR',
  'REVIEWER',
  'USER'
);

REVOKE USAGE ON TYPE nexora.membership_status, nexora.tenant_role FROM PUBLIC;
GRANT USAGE ON TYPE nexora.membership_status, nexora.tenant_role TO nexora_runtime;

DO $$
DECLARE
  api_role text;
BEGIN
  FOREACH api_role IN ARRAY ARRAY['anon', 'authenticated', 'service_role']
  LOOP
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = api_role) THEN
      EXECUTE format(
        'REVOKE USAGE ON TYPE nexora.membership_status, nexora.tenant_role FROM %I',
        api_role
      );
    END IF;
  END LOOP;
END
$$;

CREATE TABLE nexora.organizations (
  id uuid PRIMARY KEY,
  slug text NOT NULL UNIQUE
    CHECK (slug ~ '^[a-z][a-z0-9-]{2,62}$'),
  name text NOT NULL
    CHECK (char_length(name) BETWEEN 1 AND 160),
  status text NOT NULL DEFAULT 'ACTIVE'
    CHECK (status = 'ACTIVE'),
  -- This pointer and its generated constants form a deferred composite FK to
  -- one ACTIVE OWNER membership. It makes owner transfer atomic and prevents
  -- delete, suspend, or demotion of the last designated owner.
  owner_membership_id uuid NOT NULL,
  owner_membership_status nexora.membership_status
    GENERATED ALWAYS AS ('ACTIVE'::nexora.membership_status) STORED,
  owner_tenant_role nexora.tenant_role
    GENERATED ALWAYS AS ('OWNER'::nexora.tenant_role) STORED,
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  created_at timestamp with time zone NOT NULL DEFAULT transaction_timestamp(),
  updated_at timestamp with time zone NOT NULL DEFAULT transaction_timestamp(),
  CONSTRAINT organizations_updated_after_created
    CHECK (updated_at >= created_at),
  CONSTRAINT organizations_owner_tenant_key
    UNIQUE (id, owner_membership_id)
);

CREATE TABLE nexora.memberships (
  id uuid PRIMARY KEY,
  organization_id uuid NOT NULL,
  subject_id uuid NOT NULL,
  status nexora.membership_status NOT NULL,
  tenant_role nexora.tenant_role NOT NULL,
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  created_at timestamp with time zone NOT NULL DEFAULT transaction_timestamp(),
  updated_at timestamp with time zone NOT NULL DEFAULT transaction_timestamp(),
  CONSTRAINT memberships_updated_after_created
    CHECK (updated_at >= created_at),
  CONSTRAINT memberships_organization_subject_key
    UNIQUE (organization_id, subject_id),
  CONSTRAINT memberships_organization_id_id_key
    UNIQUE (organization_id, id),
  CONSTRAINT memberships_owner_reference_key
    UNIQUE (organization_id, id, status, tenant_role),
  CONSTRAINT memberships_organization_fk
    FOREIGN KEY (organization_id)
    REFERENCES nexora.organizations (id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT
    DEFERRABLE INITIALLY DEFERRED
);

ALTER TABLE nexora.organizations
  ADD CONSTRAINT organizations_active_owner_fk
  FOREIGN KEY (
    id,
    owner_membership_id,
    owner_membership_status,
    owner_tenant_role
  )
  REFERENCES nexora.memberships (
    organization_id,
    id,
    status,
    tenant_role
  )
  ON UPDATE RESTRICT
  ON DELETE RESTRICT
  DEFERRABLE INITIALLY DEFERRED;

CREATE INDEX memberships_subject_active_lookup_idx
ON nexora.memberships (subject_id, organization_id, id)
WHERE status = 'ACTIVE';

CREATE INDEX memberships_active_owner_lookup_idx
ON nexora.memberships (organization_id, id)
WHERE status = 'ACTIVE' AND tenant_role = 'OWNER';

CREATE TRIGGER organizations_advance_version
BEFORE UPDATE ON nexora.organizations
FOR EACH ROW
EXECUTE FUNCTION nexora.advance_row_version();

CREATE TRIGGER memberships_advance_version
BEFORE UPDATE ON nexora.memberships
FOR EACH ROW
EXECUTE FUNCTION nexora.advance_row_version();

ALTER TABLE nexora.organizations ENABLE ROW LEVEL SECURITY;
ALTER TABLE nexora.organizations FORCE ROW LEVEL SECURITY;
ALTER TABLE nexora.memberships ENABLE ROW LEVEL SECURITY;
ALTER TABLE nexora.memberships FORCE ROW LEVEL SECURITY;

-- Before tenant resolution, only nexora.subject_id is set and a subject may
-- enumerate its own current ACTIVE memberships. After resolution, all three
-- settings are present and only the exact authoritative membership remains
-- visible. Removed or suspended rows never authorize either phase.
CREATE POLICY memberships_select_current_subject
ON nexora.memberships
FOR SELECT
TO nexora_runtime
USING (
  status = 'ACTIVE'
  AND subject_id = NULLIF(current_setting('nexora.subject_id', true), '')::uuid
  AND (
    (
      NULLIF(current_setting('nexora.organization_id', true), '') IS NULL
      AND NULLIF(current_setting('nexora.membership_id', true), '') IS NULL
    )
    OR (
      organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
      AND id = NULLIF(current_setting('nexora.membership_id', true), '')::uuid
    )
  )
);

-- The organization root is tenant data. Its policy rechecks the current ACTIVE
-- membership row instead of trusting a selected organization or JWT metadata.
CREATE POLICY organizations_select_current_membership
ON nexora.organizations
FOR SELECT
TO nexora_runtime
USING (
  id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND EXISTS (
    SELECT 1
    FROM nexora.memberships AS current_membership
    WHERE current_membership.organization_id = organizations.id
      AND current_membership.id = NULLIF(current_setting('nexora.membership_id', true), '')::uuid
      AND current_membership.subject_id = NULLIF(current_setting('nexora.subject_id', true), '')::uuid
      AND current_membership.status = 'ACTIVE'
  )
);

GRANT SELECT (
  id,
  slug,
  name,
  status,
  owner_membership_id,
  version,
  created_at,
  updated_at
) ON nexora.organizations TO nexora_runtime;

GRANT SELECT ON nexora.memberships TO nexora_runtime;

COMMIT;
