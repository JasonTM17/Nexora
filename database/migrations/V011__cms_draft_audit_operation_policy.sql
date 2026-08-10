-- M2-DB02 forward repair: draft audit writes must use the permission that
-- matches the audited operation. Audit actor identity remains server context,
-- never a caller-supplied payload claim.

BEGIN;
SET LOCAL ROLE nexora_migrator;

DROP POLICY cms_audit_events_insert_tenant ON nexora.cms_audit_events;

CREATE POLICY cms_audit_events_insert_tenant
ON nexora.cms_audit_events FOR INSERT TO nexora_runtime
WITH CHECK (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND actor_id = NULLIF(current_setting('nexora.subject_id', true), '')::uuid
  AND (
    (operation = 'PAGE_CREATE' AND nexora.cms_current_tenant_has_permission('page.create'))
    OR (operation = 'PAGE_UPDATE' AND nexora.cms_current_tenant_has_permission('page.update'))
    OR (
      operation IN ('WORKFLOW', 'PUBLISH', 'ROLLBACK', 'THEME_PUBLISH')
      AND nexora.cms_current_tenant_has_permission('page.publish')
    )
  )
);

COMMENT ON POLICY cms_audit_events_insert_tenant ON nexora.cms_audit_events IS
  'Forced-RLS audit insert: current selected-tenant ACTIVE actor must match actor_id and hold operation-specific CMS permission.';

COMMIT;
