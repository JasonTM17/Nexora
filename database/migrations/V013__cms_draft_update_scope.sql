-- M2-DB02 forward repair: page.update is a draft-only authority. Published
-- content and publication pointers can never be changed by a state-stable
-- update, even when a caller retains page.update.

BEGIN;
SET LOCAL ROLE nexora_migrator;

-- V008 predates the publication workflow boundary and allowed every
-- page.update caller to UPDATE every visible page. Replace that broad policy
-- rather than adding another permissive policy: permissive RLS policies are
-- OR-composed.
DROP POLICY pages_update_tenant ON nexora.pages;

CREATE POLICY pages_update_draft_tenant
ON nexora.pages FOR UPDATE TO nexora_runtime
USING (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND state = 'DRAFT'
  AND nexora.cms_current_tenant_has_permission('page.update')
)
WITH CHECK (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND state = 'DRAFT'
  AND nexora.cms_current_tenant_has_permission('page.update')
);

CREATE OR REPLACE FUNCTION nexora.guard_publish_only_page_workflow()
RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = pg_catalog, nexora
AS $function$
BEGIN
  IF NEW.state IS NOT DISTINCT FROM OLD.state THEN
    -- Only DRAFT content may use the normal edit authority. The row-version
    -- trigger has already established the automatic version/timestamp values.
    IF OLD.state <> 'DRAFT'
       OR NOT nexora.cms_current_tenant_has_permission('page.update') THEN
      RAISE EXCEPTION USING
        ERRCODE = '42501',
        MESSAGE = 'page.update is limited to DRAFT page edits';
    END IF;

    IF NEW.published_version_id IS DISTINCT FROM OLD.published_version_id
       OR NEW.draft_version <> OLD.draft_version + 1
       OR NEW.version <> OLD.version + 1
       OR NEW.updated_at IS DISTINCT FROM transaction_timestamp() THEN
      RAISE EXCEPTION USING
        ERRCODE = '42501',
        MESSAGE = 'draft edits require automatic versions and cannot change the published pointer';
    END IF;

    RETURN NEW;
  END IF;

  IF NOT nexora.cms_current_tenant_has_permission('page.publish') THEN
    RAISE EXCEPTION USING
      ERRCODE = '42501',
      MESSAGE = 'page.publish is required for a workflow transition';
  END IF;

  -- The pre-existing pages_advance_version trigger runs first. A workflow
  -- transition changes no payload, tenant/site identity, SEO, theme, slug,
  -- title, publication pointer, draft version, or creation timestamp.
  IF (to_jsonb(NEW) - ARRAY['state', 'version', 'updated_at'])
       IS DISTINCT FROM (to_jsonb(OLD) - ARRAY['state', 'version', 'updated_at'])
     OR NEW.version <> OLD.version + 1
     OR NEW.updated_at IS DISTINCT FROM transaction_timestamp() THEN
    RAISE EXCEPTION USING
      ERRCODE = '42501',
      MESSAGE = 'workflow transitions may change only state and automatic version timestamps';
  END IF;

  IF NOT (
    (OLD.state = 'DRAFT' AND NEW.state = 'IN_REVIEW')
    OR (OLD.state = 'IN_REVIEW' AND NEW.state IN ('APPROVED', 'DRAFT'))
    OR (OLD.state = 'APPROVED' AND NEW.state IN ('PUBLISHED', 'DRAFT'))
    OR (OLD.state = 'PUBLISHED' AND NEW.state IN ('DRAFT', 'ARCHIVED'))
  ) THEN
    RAISE EXCEPTION USING
      ERRCODE = '42501',
      MESSAGE = 'workflow transition is not allowed by the frozen CMS lifecycle';
  END IF;

  RETURN NEW;
END
$function$;

COMMENT ON POLICY pages_update_draft_tenant ON nexora.pages IS
  'Forced-RLS page.update path: only state-stable DRAFT content with automatic version progression.';
COMMENT ON FUNCTION nexora.guard_publish_only_page_workflow() IS
  'SECURITY INVOKER C02 guard: page.update edits only DRAFT rows; page.publish transitions preserve all public payload and publication pointers.';

COMMIT;
