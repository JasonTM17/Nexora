-- M2-DB01 fixed v1 permission matrix and guarded tenant mutations.

BEGIN;
SET LOCAL ROLE nexora_migrator;

CREATE TYPE nexora.tenant_permission AS ENUM (
  'organization.read',
  'organization.update',
  'role.read',
  'role.manage',
  'page.read',
  'page.create',
  'page.update',
  'page.publish',
  'theme.read',
  'theme.update',
  'knowledge.read',
  'knowledge.manage',
  'user.manage'
);

REVOKE USAGE ON TYPE nexora.tenant_permission FROM PUBLIC;
GRANT USAGE ON TYPE nexora.tenant_permission TO nexora_runtime;

DO $$
DECLARE
  api_role text;
BEGIN
  FOREACH api_role IN ARRAY ARRAY['anon', 'authenticated', 'service_role']
  LOOP
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = api_role) THEN
      EXECUTE format('REVOKE USAGE ON TYPE nexora.tenant_permission FROM %I', api_role);
    END IF;
  END LOOP;
END
$$;

CREATE TABLE nexora.tenant_role_permissions (
  tenant_role nexora.tenant_role NOT NULL,
  permission nexora.tenant_permission NOT NULL,
  PRIMARY KEY (tenant_role, permission)
);

COMMENT ON TABLE nexora.tenant_role_permissions IS
  'Frozen M2-C01 v1 tenant permission matrix; custom tenant roles are unsupported.';

INSERT INTO nexora.tenant_role_permissions (tenant_role, permission)
VALUES
  ('OWNER', 'organization.read'),
  ('OWNER', 'organization.update'),
  ('OWNER', 'role.read'),
  ('OWNER', 'role.manage'),
  ('OWNER', 'page.read'),
  ('OWNER', 'page.create'),
  ('OWNER', 'page.update'),
  ('OWNER', 'page.publish'),
  ('OWNER', 'theme.read'),
  ('OWNER', 'theme.update'),
  ('OWNER', 'knowledge.read'),
  ('OWNER', 'knowledge.manage'),
  ('OWNER', 'user.manage'),
  ('ADMIN', 'organization.read'),
  ('ADMIN', 'organization.update'),
  ('ADMIN', 'role.read'),
  ('ADMIN', 'role.manage'),
  ('ADMIN', 'page.read'),
  ('ADMIN', 'page.create'),
  ('ADMIN', 'page.update'),
  ('ADMIN', 'page.publish'),
  ('ADMIN', 'theme.read'),
  ('ADMIN', 'theme.update'),
  ('ADMIN', 'knowledge.read'),
  ('ADMIN', 'knowledge.manage'),
  ('ADMIN', 'user.manage'),
  ('EDITOR', 'organization.read'),
  ('EDITOR', 'role.read'),
  ('EDITOR', 'page.read'),
  ('EDITOR', 'page.create'),
  ('EDITOR', 'page.update'),
  ('EDITOR', 'page.publish'),
  ('EDITOR', 'theme.read'),
  ('EDITOR', 'theme.update'),
  ('EDITOR', 'knowledge.read'),
  ('EDITOR', 'knowledge.manage'),
  ('CONTENT_CREATOR', 'organization.read'),
  ('CONTENT_CREATOR', 'page.read'),
  ('CONTENT_CREATOR', 'page.create'),
  ('CONTENT_CREATOR', 'page.update'),
  ('CONTENT_CREATOR', 'theme.read'),
  ('CONTENT_CREATOR', 'knowledge.read'),
  ('REVIEWER', 'organization.read'),
  ('REVIEWER', 'page.read'),
  ('REVIEWER', 'page.publish'),
  ('REVIEWER', 'theme.read'),
  ('REVIEWER', 'knowledge.read'),
  ('USER', 'organization.read'),
  ('USER', 'page.read'),
  ('USER', 'theme.read'),
  ('USER', 'knowledge.read');

-- Membership policies cannot safely select their own table to validate the
-- actor (PostgreSQL rejects recursive RLS). This invoker trigger performs the
-- authoritative membership and matrix checks before every write. It receives
-- no bypass and sees only the actor's exact ACTIVE membership through RLS.
CREATE FUNCTION nexora.guard_membership_mutation()
RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = pg_catalog, nexora
AS $function$
DECLARE
  target_row nexora.memberships%ROWTYPE;
  actor_role nexora.tenant_role;
  actor_subject uuid;
  actor_organization uuid;
  actor_membership uuid;
  is_bootstrap boolean := false;
BEGIN
  target_row := CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
  actor_subject := NULLIF(current_setting('nexora.subject_id', true), '')::uuid;
  actor_organization := NULLIF(current_setting('nexora.organization_id', true), '')::uuid;
  actor_membership := NULLIF(current_setting('nexora.membership_id', true), '')::uuid;

  IF TG_OP = 'INSERT'
     AND target_row.organization_id = actor_organization
     AND target_row.id = actor_membership
     AND target_row.subject_id = actor_subject
     AND target_row.status = 'ACTIVE'
     AND target_row.tenant_role = 'OWNER'
     AND EXISTS (
       SELECT 1
       FROM nexora.organizations AS bootstrap_organization
       WHERE bootstrap_organization.id = target_row.organization_id
         AND bootstrap_organization.owner_membership_id = target_row.id
     ) THEN
    is_bootstrap := true;
  END IF;

  IF NOT is_bootstrap THEN
    SELECT current_membership.tenant_role
    INTO actor_role
    FROM nexora.memberships AS current_membership
    WHERE current_membership.organization_id = actor_organization
      AND current_membership.id = actor_membership
      AND current_membership.subject_id = actor_subject
      AND current_membership.status = 'ACTIVE';

    IF actor_role IS NULL OR target_row.organization_id <> actor_organization THEN
      RAISE EXCEPTION USING
        ERRCODE = '42501',
        MESSAGE = 'current ACTIVE membership context is required';
    END IF;

    IF NOT EXISTS (
      SELECT 1
      FROM nexora.tenant_role_permissions
      WHERE tenant_role = actor_role
        AND permission = 'user.manage'
    ) OR NOT EXISTS (
      SELECT 1
      FROM nexora.tenant_role_permissions
      WHERE tenant_role = actor_role
        AND permission = 'role.manage'
    ) THEN
      RAISE EXCEPTION USING
        ERRCODE = '42501',
        MESSAGE = 'user.manage and role.manage are required';
    END IF;

    IF target_row.tenant_role = 'OWNER' AND actor_role <> 'OWNER' THEN
      RAISE EXCEPTION USING
        ERRCODE = '42501',
        MESSAGE = 'only OWNER may assign OWNER';
    END IF;

    IF EXISTS (
      SELECT target_permission.permission
      FROM nexora.tenant_role_permissions AS target_permission
      WHERE target_permission.tenant_role = target_row.tenant_role
      EXCEPT
      SELECT actor_permission.permission
      FROM nexora.tenant_role_permissions AS actor_permission
      WHERE actor_permission.tenant_role = actor_role
    ) THEN
      RAISE EXCEPTION USING
        ERRCODE = '42501',
        MESSAGE = 'actor may not assign permissions it does not hold';
    END IF;
  END IF;

  IF TG_OP = 'DELETE' THEN
    RETURN OLD;
  END IF;
  RETURN NEW;
END
$function$;

REVOKE ALL ON FUNCTION nexora.guard_membership_mutation() FROM PUBLIC;

CREATE TRIGGER memberships_guard_mutation
BEFORE INSERT OR UPDATE OR DELETE ON nexora.memberships
FOR EACH ROW
EXECUTE FUNCTION nexora.guard_membership_mutation();

CREATE FUNCTION nexora.guard_owner_transfer()
RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = pg_catalog, nexora
AS $function$
DECLARE
  actor_role nexora.tenant_role;
BEGIN
  IF NEW.owner_membership_id IS NOT DISTINCT FROM OLD.owner_membership_id THEN
    RETURN NEW;
  END IF;

  SELECT current_membership.tenant_role
  INTO actor_role
  FROM nexora.memberships AS current_membership
  WHERE current_membership.organization_id = OLD.id
    AND current_membership.id = NULLIF(current_setting('nexora.membership_id', true), '')::uuid
    AND current_membership.subject_id = NULLIF(current_setting('nexora.subject_id', true), '')::uuid
    AND current_membership.status = 'ACTIVE';

  IF actor_role IS DISTINCT FROM 'OWNER'::nexora.tenant_role THEN
    RAISE EXCEPTION USING
      ERRCODE = '42501',
      MESSAGE = 'only OWNER may transfer the designated active owner';
  END IF;

  RETURN NEW;
END
$function$;

REVOKE ALL ON FUNCTION nexora.guard_owner_transfer() FROM PUBLIC;

CREATE TRIGGER organizations_guard_owner_transfer
BEFORE UPDATE OF owner_membership_id ON nexora.organizations
FOR EACH ROW
EXECUTE FUNCTION nexora.guard_owner_transfer();

-- A just-created organization is visible only to the exact transaction-local
-- subject/membership context named as its pending owner and only while the row
-- still belongs to the inserting transaction. A committed organization cannot
-- re-enter this bootstrap path even when its id and owner membership id are
-- known. This permits the deferred organization + first OWNER membership
-- bootstrap in one transaction without weakening committed tenant reads.
CREATE POLICY organizations_select_pending_bootstrap
ON nexora.organizations
FOR SELECT
TO nexora_runtime
USING (
  xmin = pg_current_xact_id()::xid
  AND id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND owner_membership_id = NULLIF(current_setting('nexora.membership_id', true), '')::uuid
  AND NULLIF(current_setting('nexora.subject_id', true), '') IS NOT NULL
);

CREATE POLICY organizations_bootstrap_insert
ON nexora.organizations
FOR INSERT
TO nexora_runtime
WITH CHECK (
  id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND owner_membership_id = NULLIF(current_setting('nexora.membership_id', true), '')::uuid
  AND NULLIF(current_setting('nexora.subject_id', true), '') IS NOT NULL
);

CREATE POLICY organizations_update_with_permission
ON nexora.organizations
FOR UPDATE
TO nexora_runtime
USING (
  id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND EXISTS (
    SELECT 1
    FROM nexora.memberships AS current_membership
    JOIN nexora.tenant_role_permissions AS granted_permission
      ON granted_permission.tenant_role = current_membership.tenant_role
     AND granted_permission.permission = 'organization.update'
    WHERE current_membership.organization_id = organizations.id
      AND current_membership.id = NULLIF(current_setting('nexora.membership_id', true), '')::uuid
      AND current_membership.subject_id = NULLIF(current_setting('nexora.subject_id', true), '')::uuid
      AND current_membership.status = 'ACTIVE'
  )
)
WITH CHECK (
  id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND EXISTS (
    SELECT 1
    FROM nexora.memberships AS current_membership
    JOIN nexora.tenant_role_permissions AS granted_permission
      ON granted_permission.tenant_role = current_membership.tenant_role
     AND granted_permission.permission = 'organization.update'
    WHERE current_membership.organization_id = organizations.id
      AND current_membership.id = NULLIF(current_setting('nexora.membership_id', true), '')::uuid
      AND current_membership.subject_id = NULLIF(current_setting('nexora.subject_id', true), '')::uuid
      AND current_membership.status = 'ACTIVE'
  )
);

CREATE POLICY memberships_insert_tenant_target
ON nexora.memberships
FOR INSERT
TO nexora_runtime
WITH CHECK (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
);

CREATE POLICY memberships_update_tenant_target
ON nexora.memberships
FOR UPDATE
TO nexora_runtime
USING (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
)
WITH CHECK (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
);

GRANT SELECT ON nexora.tenant_role_permissions TO nexora_runtime;
GRANT INSERT (
  id,
  slug,
  name,
  status,
  owner_membership_id
) ON nexora.organizations TO nexora_runtime;
GRANT UPDATE (
  name,
  owner_membership_id
) ON nexora.organizations TO nexora_runtime;
GRANT INSERT (
  id,
  organization_id,
  subject_id,
  status,
  tenant_role
) ON nexora.memberships TO nexora_runtime;
GRANT UPDATE (
  status,
  tenant_role
) ON nexora.memberships TO nexora_runtime;

COMMIT;
