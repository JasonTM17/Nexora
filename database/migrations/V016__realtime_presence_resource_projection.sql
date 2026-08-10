-- M3-DB01 forward repair: security-definer Realtime policy helpers must not
-- bypass or weaken FORCE RLS on CMS pages. This private projection preserves
-- only page-to-organization identity for server-issued Presence descriptors.

BEGIN;
SET LOCAL ROLE nexora_migrator;

CREATE TABLE nexora.realtime_presence_resources (
  resource_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL,
  updated_at timestamptz NOT NULL DEFAULT transaction_timestamp(),
  CONSTRAINT realtime_presence_resources_page_fk
    FOREIGN KEY (organization_id, resource_id)
    REFERENCES nexora.pages (organization_id, id)
    ON UPDATE RESTRICT
    ON DELETE CASCADE
);

COMMENT ON TABLE nexora.realtime_presence_resources IS
  'Private page-resource to organization projection for scoped Realtime Presence descriptors. It exposes no page content and is maintained only by a page trigger.';

ALTER TABLE nexora.realtime_presence_resources ENABLE ROW LEVEL SECURITY;
ALTER TABLE nexora.realtime_presence_resources FORCE ROW LEVEL SECURITY;

CREATE POLICY realtime_presence_resources_migrator_control
ON nexora.realtime_presence_resources
FOR ALL
TO nexora_migrator
USING (true)
WITH CHECK (true);

CREATE POLICY realtime_presence_resources_trigger_write
ON nexora.realtime_presence_resources
FOR ALL
TO nexora_runtime
USING (pg_trigger_depth() > 0)
WITH CHECK (pg_trigger_depth() > 0);

GRANT INSERT, UPDATE, DELETE ON nexora.realtime_presence_resources TO nexora_runtime;

CREATE POLICY pages_realtime_presence_backfill_select
ON nexora.pages
FOR SELECT
TO nexora_migrator
USING (true);

INSERT INTO nexora.realtime_presence_resources (resource_id, organization_id)
SELECT id, organization_id
FROM nexora.pages;

DROP POLICY pages_realtime_presence_backfill_select ON nexora.pages;

CREATE FUNCTION nexora.sync_realtime_presence_resource()
RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = pg_catalog, nexora
AS $function$
BEGIN
  IF TG_OP = 'DELETE' THEN
    DELETE FROM nexora.realtime_presence_resources
    WHERE resource_id = OLD.id;
    RETURN OLD;
  END IF;

  INSERT INTO nexora.realtime_presence_resources (
    resource_id,
    organization_id,
    updated_at
  ) VALUES (
    NEW.id,
    NEW.organization_id,
    transaction_timestamp()
  ) ON CONFLICT (resource_id) DO UPDATE
  SET organization_id = EXCLUDED.organization_id,
      updated_at = EXCLUDED.updated_at;

  RETURN NEW;
END
$function$;

REVOKE ALL ON FUNCTION nexora.sync_realtime_presence_resource() FROM PUBLIC;

CREATE TRIGGER pages_sync_realtime_presence_resource
AFTER INSERT OR UPDATE OR DELETE ON nexora.pages
FOR EACH ROW
EXECUTE FUNCTION nexora.sync_realtime_presence_resource();

CREATE OR REPLACE FUNCTION nexora.realtime_private_channel_authorized(in_topic text, in_subject_id uuid)
RETURNS boolean
LANGUAGE plpgsql
STABLE
STRICT
SECURITY DEFINER
SET search_path = pg_catalog, nexora
AS $function$
DECLARE
  parts text[];
  owner_id uuid;
BEGIN
  parts := string_to_array(in_topic, ':');
  IF array_length(parts, 1) <> 3
     OR parts[1] NOT IN ('tenant', 'resource')
     OR parts[2] !~* '^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$'
     OR parts[3] NOT IN ('publication', 'workflow', 'job-progress', 'presence') THEN
    RETURN false;
  END IF;
  owner_id := parts[2]::uuid;

  IF parts[1] = 'tenant' THEN
    RETURN parts[3] IN ('publication', 'workflow')
      AND EXISTS (
        SELECT 1
        FROM nexora.membership_authorizations AS actor
        WHERE actor.organization_id = owner_id
          AND actor.subject_id = in_subject_id
          AND actor.status = 'ACTIVE'
      );
  END IF;

  IF parts[3] = 'job-progress' THEN
    RETURN EXISTS (
      SELECT 1
      FROM nexora.outbox_events AS event
      JOIN nexora.membership_authorizations AS actor
        ON actor.organization_id = event.organization_id
       AND actor.subject_id = in_subject_id
       AND actor.status = 'ACTIVE'
      WHERE event.topic = in_topic
        AND event.event_type = 'JOB_PROGRESS_CHANGED'
        AND event.resource_id = owner_id
        AND event.subject_id = in_subject_id
    );
  END IF;

  RETURN EXISTS (
    SELECT 1
    FROM nexora.realtime_presence_resources AS resource
    JOIN nexora.membership_authorizations AS actor
      ON actor.organization_id = resource.organization_id
     AND actor.subject_id = in_subject_id
     AND actor.status = 'ACTIVE'
    WHERE resource.resource_id = owner_id
  );
END
$function$;

REVOKE ALL ON FUNCTION nexora.realtime_private_channel_authorized(text, uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION nexora.realtime_private_channel_authorized(text, uuid) TO nexora_runtime;

COMMIT;
