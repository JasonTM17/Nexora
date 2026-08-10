-- M2-DB02 forward repair: a publish-capable reviewer may perform a legitimate
-- server workflow state change without receiving general draft-edit authority.

BEGIN;
SET LOCAL ROLE nexora_migrator;

CREATE FUNCTION nexora.guard_publish_only_page_workflow()
RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = pg_catalog, nexora
AS $function$
BEGIN
  -- State-stable updates remain the normal page.update path. The separate
  -- publish policy never turns page.publish into content or SEO edit access.
  IF NEW.state IS NOT DISTINCT FROM OLD.state THEN
    IF NOT nexora.cms_current_tenant_has_permission('page.update') THEN
      RAISE EXCEPTION USING
        ERRCODE = '42501',
        MESSAGE = 'page.update is required for a draft edit';
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

REVOKE ALL ON FUNCTION nexora.guard_publish_only_page_workflow() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION nexora.guard_publish_only_page_workflow() TO nexora_runtime;

CREATE TRIGGER pages_publish_workflow_guard
BEFORE UPDATE ON nexora.pages
FOR EACH ROW EXECUTE FUNCTION nexora.guard_publish_only_page_workflow();

CREATE POLICY pages_publish_workflow_transition
ON nexora.pages FOR UPDATE TO nexora_runtime
USING (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.cms_current_tenant_has_permission('page.publish')
)
WITH CHECK (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.cms_current_tenant_has_permission('page.publish')
);

COMMENT ON FUNCTION nexora.guard_publish_only_page_workflow() IS
  'SECURITY INVOKER C02 state-graph guard: page.publish permits only a payload-immutable lifecycle transition.';
COMMENT ON POLICY pages_publish_workflow_transition ON nexora.pages IS
  'Forced-RLS page.publish transition path; trigger restricts it to frozen lifecycle states and automatic version timestamps.';

COMMIT;
