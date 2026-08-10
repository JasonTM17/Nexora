-- M2-DB01 bounded manager membership enumeration for M2-U02.
--
-- This is a forward policy only. It does not weaken the actor-only or exact
-- target policies from V003/V005, and it uses the same forced-RLS projection
-- to avoid recursive membership policy evaluation.

BEGIN;
SET LOCAL ROLE nexora_migrator;

-- Runtime list responses may contain only the fields declared by the M2
-- membership read contract. Keep other membership columns unavailable even to
-- a permitted manager.
REVOKE SELECT ON nexora.memberships FROM nexora_runtime;
GRANT SELECT (id, organization_id, subject_id, status, tenant_role, version)
ON nexora.memberships TO nexora_runtime;

CREATE POLICY memberships_select_manager_tenant
ON nexora.memberships
FOR SELECT
TO nexora_runtime
USING (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
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

COMMENT ON POLICY memberships_select_manager_tenant ON nexora.memberships IS
  'Current ACTIVE user.manage plus role.manage actor may enumerate only selected-tenant membership rows.';

COMMIT;
