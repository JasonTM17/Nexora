-- M2-DB02 runtime access for V007. Every policy is scoped to the current
-- transaction-local tenant context and the frozen M2 permission matrix.

BEGIN;
SET LOCAL ROLE nexora_migrator;

CREATE FUNCTION nexora.cms_current_tenant_has_permission(
  required_permission nexora.tenant_permission
)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY INVOKER
SET search_path = pg_catalog, nexora
AS $function$
  SELECT EXISTS (
    SELECT 1
    FROM nexora.membership_authorizations AS actor
    JOIN nexora.tenant_role_permissions AS granted
      ON granted.tenant_role = actor.tenant_role
     AND granted.permission = required_permission
    WHERE actor.membership_id = NULLIF(current_setting('nexora.membership_id', true), '')::uuid
      AND actor.organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
      AND actor.subject_id = NULLIF(current_setting('nexora.subject_id', true), '')::uuid
      AND actor.status = 'ACTIVE'
  )
$function$;

REVOKE ALL ON FUNCTION nexora.cms_current_tenant_has_permission(nexora.tenant_permission) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION nexora.cms_current_tenant_has_permission(nexora.tenant_permission) TO nexora_runtime;

-- Every tenant-scoped CMS table must match the resolved organization. The
-- helper re-reads the exact ACTIVE actor projection under forced RLS; a header,
-- JWT metadata, page ID, or site ID never authorizes access by itself.
CREATE POLICY sites_select_tenant
ON nexora.sites FOR SELECT TO nexora_runtime
USING (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.cms_current_tenant_has_permission('organization.read')
);

CREATE POLICY sites_insert_tenant
ON nexora.sites FOR INSERT TO nexora_runtime
WITH CHECK (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.cms_current_tenant_has_permission('organization.update')
);

CREATE POLICY sites_update_tenant
ON nexora.sites FOR UPDATE TO nexora_runtime
USING (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.cms_current_tenant_has_permission('organization.update')
)
WITH CHECK (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.cms_current_tenant_has_permission('organization.update')
);

CREATE POLICY themes_select_tenant
ON nexora.themes FOR SELECT TO nexora_runtime
USING (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.cms_current_tenant_has_permission('theme.read')
);

CREATE POLICY themes_write_tenant
ON nexora.themes FOR INSERT TO nexora_runtime
WITH CHECK (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.cms_current_tenant_has_permission('theme.update')
);

CREATE POLICY themes_update_tenant
ON nexora.themes FOR UPDATE TO nexora_runtime
USING (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.cms_current_tenant_has_permission('theme.update')
)
WITH CHECK (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.cms_current_tenant_has_permission('theme.update')
);

CREATE POLICY theme_versions_select_tenant
ON nexora.theme_versions FOR SELECT TO nexora_runtime
USING (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.cms_current_tenant_has_permission('theme.read')
);

CREATE POLICY theme_versions_insert_tenant
ON nexora.theme_versions FOR INSERT TO nexora_runtime
WITH CHECK (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.cms_current_tenant_has_permission('theme.update')
);

CREATE POLICY media_assets_select_tenant
ON nexora.media_assets FOR SELECT TO nexora_runtime
USING (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.cms_current_tenant_has_permission('page.read')
);

CREATE POLICY pages_select_tenant
ON nexora.pages FOR SELECT TO nexora_runtime
USING (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.cms_current_tenant_has_permission('page.read')
);

CREATE POLICY pages_insert_tenant
ON nexora.pages FOR INSERT TO nexora_runtime
WITH CHECK (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.cms_current_tenant_has_permission('page.create')
);

CREATE POLICY pages_update_tenant
ON nexora.pages FOR UPDATE TO nexora_runtime
USING (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.cms_current_tenant_has_permission('page.update')
)
WITH CHECK (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.cms_current_tenant_has_permission('page.update')
);

CREATE POLICY page_versions_select_tenant
ON nexora.page_versions FOR SELECT TO nexora_runtime
USING (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.cms_current_tenant_has_permission('page.read')
);

CREATE POLICY page_versions_insert_tenant
ON nexora.page_versions FOR INSERT TO nexora_runtime
WITH CHECK (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.cms_current_tenant_has_permission('page.publish')
);

CREATE POLICY page_publications_select_tenant
ON nexora.page_publications FOR SELECT TO nexora_runtime
USING (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.cms_current_tenant_has_permission('page.read')
);

CREATE POLICY page_publications_insert_tenant
ON nexora.page_publications FOR INSERT TO nexora_runtime
WITH CHECK (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.cms_current_tenant_has_permission('page.publish')
);

CREATE POLICY workflow_reviews_select_tenant
ON nexora.workflow_reviews FOR SELECT TO nexora_runtime
USING (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.cms_current_tenant_has_permission('page.read')
);

CREATE POLICY workflow_reviews_insert_tenant
ON nexora.workflow_reviews FOR INSERT TO nexora_runtime
WITH CHECK (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.cms_current_tenant_has_permission('page.publish')
);

CREATE POLICY cms_audit_events_select_tenant
ON nexora.cms_audit_events FOR SELECT TO nexora_runtime
USING (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.cms_current_tenant_has_permission('page.read')
);

CREATE POLICY cms_audit_events_insert_tenant
ON nexora.cms_audit_events FOR INSERT TO nexora_runtime
WITH CHECK (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.cms_current_tenant_has_permission('page.publish')
);

GRANT SELECT, INSERT, UPDATE ON nexora.sites TO nexora_runtime;
GRANT SELECT, INSERT, UPDATE
ON nexora.themes TO nexora_runtime;
GRANT SELECT, INSERT
ON nexora.theme_versions TO nexora_runtime;
GRANT SELECT
ON nexora.media_assets TO nexora_runtime;
GRANT SELECT, INSERT, UPDATE
ON nexora.pages TO nexora_runtime;
GRANT SELECT, INSERT
ON nexora.page_versions TO nexora_runtime;
GRANT SELECT, INSERT
ON nexora.page_publications TO nexora_runtime;
GRANT SELECT, INSERT
ON nexora.workflow_reviews TO nexora_runtime;
GRANT SELECT, INSERT
ON nexora.cms_audit_events TO nexora_runtime;

DO $$
DECLARE
  api_role text;
  relation_name text;
BEGIN
  FOREACH api_role IN ARRAY ARRAY['anon', 'authenticated', 'service_role']
  LOOP
    FOREACH relation_name IN ARRAY ARRAY[
      'nexora.sites',
      'nexora.themes',
      'nexora.theme_versions',
      'nexora.media_assets',
      'nexora.pages',
      'nexora.page_versions',
      'nexora.page_publications',
      'nexora.workflow_reviews',
      'nexora.cms_audit_events'
    ]
    LOOP
      IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = api_role) THEN
        EXECUTE format('REVOKE ALL ON %s FROM %I', relation_name, api_role);
      END IF;
    END LOOP;
  END LOOP;
END
$$;

COMMENT ON FUNCTION nexora.cms_current_tenant_has_permission(nexora.tenant_permission) IS
  'SECURITY INVOKER forced-RLS helper: current ACTIVE actor plus frozen role permission only; no bypass or browser authority.';

COMMIT;
