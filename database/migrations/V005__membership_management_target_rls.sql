-- M2-DB01 bounded target-membership authorization for M2-T02.
--
-- PostgreSQL RLS policies on memberships cannot inspect memberships again to
-- authorize the actor: that is recursive RLS.  This private, forced-RLS
-- projection is synchronously maintained by an invoker trigger, so target-row
-- policy evaluation can re-read the current actor without a definer or bypass.

BEGIN;
SET LOCAL ROLE nexora_migrator;

CREATE TABLE nexora.membership_authorizations (
  membership_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL,
  subject_id uuid NOT NULL,
  status nexora.membership_status NOT NULL,
  tenant_role nexora.tenant_role NOT NULL,
  membership_version bigint NOT NULL CHECK (membership_version > 0),
  CONSTRAINT membership_authorizations_membership_fk
    FOREIGN KEY (organization_id, membership_id)
    REFERENCES nexora.memberships (organization_id, id)
    ON UPDATE RESTRICT
    ON DELETE CASCADE
);

COMMENT ON TABLE nexora.membership_authorizations IS
  'Private synchronous authorization projection for bounded membership management RLS; never a runtime enumeration surface.';

-- Backfill pre-existing rows while this migration is atomically applying. The
-- temporary policy exists only for the non-login migration role and is removed
-- before commit; runtime never receives this visibility.
CREATE POLICY memberships_migrator_backfill_select
ON nexora.memberships
FOR SELECT
TO nexora_migrator
USING (true);

INSERT INTO nexora.membership_authorizations (
  membership_id,
  organization_id,
  subject_id,
  status,
  tenant_role,
  membership_version
)
SELECT id, organization_id, subject_id, status, tenant_role, version
FROM nexora.memberships;

DROP POLICY memberships_migrator_backfill_select ON nexora.memberships;

ALTER TABLE nexora.membership_authorizations ENABLE ROW LEVEL SECURITY;
ALTER TABLE nexora.membership_authorizations FORCE ROW LEVEL SECURITY;

-- Runtime code may read only the currently resolved, ACTIVE actor projection.
-- This is sufficient for the membership target policy below to check the
-- frozen matrix, and is deliberately not a tenant-member list.
CREATE POLICY membership_authorizations_select_current_actor
ON nexora.membership_authorizations
FOR SELECT
TO nexora_runtime
USING (
  membership_id = NULLIF(current_setting('nexora.membership_id', true), '')::uuid
  AND organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND subject_id = NULLIF(current_setting('nexora.subject_id', true), '')::uuid
  AND status = 'ACTIVE'
);

-- The synchronization trigger is the only runtime write path. pg_trigger_depth
-- cannot be made positive by a direct SQL statement, so explicit DML grants do
-- not turn this internal projection into a caller-writable table.
CREATE POLICY membership_authorizations_trigger_write
ON nexora.membership_authorizations
FOR ALL
TO nexora_runtime
USING (pg_trigger_depth() > 0)
WITH CHECK (pg_trigger_depth() > 0);

GRANT SELECT, INSERT, UPDATE, DELETE ON nexora.membership_authorizations TO nexora_runtime;

CREATE FUNCTION nexora.sync_membership_authorization()
RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = pg_catalog, nexora
AS $function$
BEGIN
  IF TG_OP = 'DELETE' THEN
    DELETE FROM nexora.membership_authorizations
    WHERE membership_id = OLD.id;
    RETURN OLD;
  END IF;

  INSERT INTO nexora.membership_authorizations (
    membership_id,
    organization_id,
    subject_id,
    status,
    tenant_role,
    membership_version
  )
  VALUES (
    NEW.id,
    NEW.organization_id,
    NEW.subject_id,
    NEW.status,
    NEW.tenant_role,
    NEW.version
  )
  ON CONFLICT (membership_id) DO UPDATE
  SET organization_id = EXCLUDED.organization_id,
      subject_id = EXCLUDED.subject_id,
      status = EXCLUDED.status,
      tenant_role = EXCLUDED.tenant_role,
      membership_version = EXCLUDED.membership_version;

  RETURN NEW;
END
$function$;

REVOKE ALL ON FUNCTION nexora.sync_membership_authorization() FROM PUBLIC;

CREATE TRIGGER memberships_sync_membership_authorization
AFTER INSERT OR UPDATE OR DELETE ON nexora.memberships
FOR EACH ROW
EXECUTE FUNCTION nexora.sync_membership_authorization();

-- M2-T02 supplies both target settings with set_config(..., true) only after
-- TenantContextService has freshly validated and promoted the actor context.
-- Missing, cross-tenant, stale-version, or non-management settings therefore
-- yield no target row. A query without a target still exposes only the actor's
-- own membership through the pre-existing current-subject policy. PostgreSQL
-- evaluates SELECT RLS for the post-update image too, so the one-step successor
-- is visible only while the backend's mutation marker is true for that single
-- UPDATE statement; it must be cleared immediately afterward.
CREATE POLICY memberships_select_management_target
ON nexora.memberships
FOR SELECT
TO nexora_runtime
USING (
  id = NULLIF(current_setting('nexora.target_membership_id', true), '')::uuid
  AND (
    version = NULLIF(current_setting('nexora.target_membership_version', true), '')::bigint
    OR (
      current_setting('nexora.target_membership_mutation', true) = 'true'
      AND version = NULLIF(current_setting('nexora.target_membership_version', true), '')::bigint + 1
    )
  )
  AND organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND EXISTS (
    SELECT 1
    FROM nexora.membership_authorizations AS actor
    WHERE actor.membership_id = NULLIF(current_setting('nexora.membership_id', true), '')::uuid
      AND actor.organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
      AND actor.subject_id = NULLIF(current_setting('nexora.subject_id', true), '')::uuid
      AND actor.status = 'ACTIVE'
      AND EXISTS (
        SELECT 1
        FROM nexora.tenant_role_permissions
        WHERE tenant_role = actor.tenant_role
          AND permission = 'user.manage'
      )
      AND EXISTS (
        SELECT 1
        FROM nexora.tenant_role_permissions
        WHERE tenant_role = actor.tenant_role
          AND permission = 'role.manage'
      )
  )
);

-- UPDATE needs a matching SELECT policy. Replace the former tenant-only policy
-- so every runtime membership mutation is also tied to one expected target row
-- and version. The existing guard trigger retains assignment/escalation checks;
-- the deferred owner FK continues to enforce the last-owner invariant.
DROP POLICY memberships_update_tenant_target ON nexora.memberships;

CREATE POLICY memberships_update_tenant_target
ON nexora.memberships
FOR UPDATE
TO nexora_runtime
USING (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND id = NULLIF(current_setting('nexora.target_membership_id', true), '')::uuid
  AND version = NULLIF(current_setting('nexora.target_membership_version', true), '')::bigint
)
WITH CHECK (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND version = NULLIF(current_setting('nexora.target_membership_version', true), '')::bigint + 1
);

COMMIT;
