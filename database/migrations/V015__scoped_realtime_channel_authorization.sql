-- M3-DB01 forward repair: a normal browser session is never a private
-- Realtime channel credential.  The provider verifies JWT signatures before
-- policy evaluation; these policies additionally require a short-lived,
-- same-origin-server-issued descriptor claim set bound to one topic and the
-- subject's current authorization epoch.
--
-- This migration owns application objects in nexora plus documented policy
-- DDL on realtime.messages only.  It creates no managed-schema relation,
-- function, index, trigger, grant, or configuration.

BEGIN;
SET LOCAL ROLE nexora_migrator;

CREATE TABLE nexora.realtime_authorization_epochs (
  subject_id uuid PRIMARY KEY,
  authorization_epoch bigint NOT NULL DEFAULT 1
    CHECK (authorization_epoch > 0),
  updated_at timestamptz NOT NULL DEFAULT transaction_timestamp()
);

COMMENT ON TABLE nexora.realtime_authorization_epochs IS
  'Private subject-scoped epoch invalidating server-issued Realtime channel descriptors after membership changes. It is not browser-readable authority.';

ALTER TABLE nexora.realtime_authorization_epochs ENABLE ROW LEVEL SECURITY;
ALTER TABLE nexora.realtime_authorization_epochs FORCE ROW LEVEL SECURITY;

CREATE POLICY realtime_authorization_epochs_migrator_control
ON nexora.realtime_authorization_epochs
FOR ALL
TO nexora_migrator
USING (true)
WITH CHECK (true);

-- Only the membership trigger can mutate this projection under the runtime
-- role.  A direct statement cannot manufacture pg_trigger_depth() > 0.
CREATE POLICY realtime_authorization_epochs_trigger_write
ON nexora.realtime_authorization_epochs
FOR ALL
TO nexora_runtime
USING (pg_trigger_depth() > 0)
WITH CHECK (pg_trigger_depth() > 0);

GRANT INSERT, UPDATE, DELETE ON nexora.realtime_authorization_epochs TO nexora_runtime;

CREATE POLICY memberships_realtime_epoch_backfill_select
ON nexora.memberships
FOR SELECT
TO nexora_migrator
USING (true);

INSERT INTO nexora.realtime_authorization_epochs (subject_id, authorization_epoch)
SELECT DISTINCT subject_id, 1
FROM nexora.memberships
ON CONFLICT (subject_id) DO NOTHING;

DROP POLICY memberships_realtime_epoch_backfill_select ON nexora.memberships;

CREATE FUNCTION nexora.bump_realtime_authorization_epoch()
RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = pg_catalog, nexora
AS $function$
BEGIN
  IF TG_OP = 'DELETE' THEN
    INSERT INTO nexora.realtime_authorization_epochs (
      subject_id,
      authorization_epoch,
      updated_at
    ) VALUES (
      OLD.subject_id,
      1,
      transaction_timestamp()
    ) ON CONFLICT (subject_id) DO UPDATE
    SET authorization_epoch = nexora.realtime_authorization_epochs.authorization_epoch + 1,
        updated_at = EXCLUDED.updated_at;
    RETURN OLD;
  END IF;

  INSERT INTO nexora.realtime_authorization_epochs (
    subject_id,
    authorization_epoch,
    updated_at
  ) VALUES (
    NEW.subject_id,
    1,
    transaction_timestamp()
  ) ON CONFLICT (subject_id) DO UPDATE
  SET authorization_epoch = nexora.realtime_authorization_epochs.authorization_epoch + 1,
      updated_at = EXCLUDED.updated_at;

  IF TG_OP = 'UPDATE' AND OLD.subject_id IS DISTINCT FROM NEW.subject_id THEN
    INSERT INTO nexora.realtime_authorization_epochs (
      subject_id,
      authorization_epoch,
      updated_at
    ) VALUES (
      OLD.subject_id,
      1,
      transaction_timestamp()
    ) ON CONFLICT (subject_id) DO UPDATE
    SET authorization_epoch = nexora.realtime_authorization_epochs.authorization_epoch + 1,
        updated_at = EXCLUDED.updated_at;
  END IF;

  RETURN NEW;
END
$function$;

REVOKE ALL ON FUNCTION nexora.bump_realtime_authorization_epoch() FROM PUBLIC;

CREATE TRIGGER memberships_bump_realtime_authorization_epoch
AFTER INSERT OR UPDATE OR DELETE ON nexora.memberships
FOR EACH ROW
EXECUTE FUNCTION nexora.bump_realtime_authorization_epoch();

-- V014's two-argument helper remains a private runtime-only building block.
-- Tighten its namespace so a descriptor cannot authorize notification/outbox
-- or arbitrary resource rows.  Job progress is owned by the subject recorded
-- in its durable event.  Presence is limited to a current page resource in
-- the subject's active organization; M3-T03 supplies any narrower page-view
-- or page-edit decision before issuing the descriptor.
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
    FROM nexora.pages AS page
    JOIN nexora.membership_authorizations AS actor
      ON actor.organization_id = page.organization_id
     AND actor.subject_id = in_subject_id
     AND actor.status = 'ACTIVE'
    WHERE page.id = owner_id
  );
END
$function$;

CREATE OR REPLACE FUNCTION nexora.realtime_current_channel_authorized(in_topic text)
RETURNS boolean
LANGUAGE plpgsql
STABLE
STRICT
SECURITY DEFINER
SET search_path = pg_catalog, nexora
AS $function$
DECLARE
  current_topic text;
  current_subject uuid;
  claims jsonb;
  expected_epoch bigint;
  expected_event_type text;
  parts text[];
BEGIN
  current_topic := realtime.topic();
  current_subject := auth.uid();
  claims := auth.jwt();

  IF current_topic IS NULL
     OR current_subject IS NULL
     OR jsonb_typeof(claims) <> 'object'
     OR in_topic IS DISTINCT FROM current_topic
     OR claims ->> 'sub' IS DISTINCT FROM current_subject::text
     OR claims ->> 'nexora_realtime_topic' IS DISTINCT FROM in_topic
     OR coalesce(claims ->> 'nexora_realtime_event_version', '') !~ '^[1-9][0-9]{0,18}$'
     OR coalesce(claims ->> 'nexora_realtime_authorization_epoch', '') !~ '^[1-9][0-9]{0,18}$' THEN
    RETURN false;
  END IF;

  parts := string_to_array(in_topic, ':');
  expected_event_type := CASE
    WHEN array_length(parts, 1) = 3 AND parts[1] = 'tenant' AND parts[3] = 'publication'
      THEN 'PUBLICATION_INVALIDATED'
    WHEN array_length(parts, 1) = 3 AND parts[1] = 'tenant' AND parts[3] = 'workflow'
      THEN 'WORKFLOW_TRANSITIONED'
    WHEN array_length(parts, 1) = 3 AND parts[1] = 'resource' AND parts[3] = 'job-progress'
      THEN 'JOB_PROGRESS_CHANGED'
    WHEN array_length(parts, 1) = 3 AND parts[1] = 'resource' AND parts[3] = 'presence'
      THEN 'PRESENCE_CHANGED'
    ELSE NULL
  END;

  IF expected_event_type IS NULL
     OR claims ->> 'nexora_realtime_event_type' IS DISTINCT FROM expected_event_type THEN
    RETURN false;
  END IF;

  SELECT epoch.authorization_epoch INTO expected_epoch
  FROM nexora.realtime_authorization_epochs AS epoch
  WHERE epoch.subject_id = current_subject;

  IF expected_epoch IS NULL
     OR claims ->> 'nexora_realtime_authorization_epoch' IS DISTINCT FROM expected_epoch::text THEN
    RETURN false;
  END IF;

  RETURN nexora.realtime_private_channel_authorized(in_topic, current_subject);
EXCEPTION
  WHEN invalid_text_representation OR numeric_value_out_of_range THEN
    RETURN false;
END
$function$;

REVOKE ALL ON FUNCTION nexora.realtime_private_channel_authorized(text, uuid) FROM PUBLIC;
REVOKE ALL ON FUNCTION nexora.realtime_current_channel_authorized(text) FROM PUBLIC;
REVOKE ALL ON FUNCTION nexora.bump_realtime_authorization_epoch() FROM PUBLIC;

GRANT EXECUTE ON FUNCTION nexora.realtime_private_channel_authorized(text, uuid) TO nexora_runtime;
GRANT EXECUTE ON FUNCTION nexora.realtime_current_channel_authorized(text) TO nexora_runtime;

DO $grant_authenticated$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
    GRANT EXECUTE ON FUNCTION nexora.realtime_current_channel_authorized(text) TO authenticated;
  END IF;
END
$grant_authenticated$;

-- Provider-managed schema: only the documented policy table is touched.  A
-- descriptor has read-only channel authority.  Direct browser broadcast and
-- presence writes receive no INSERT policy; M3-T03 must send a bounded
-- same-origin intent to its server adapter instead.
RESET ROLE;

DO $realtime_policy$
BEGIN
  IF to_regclass('realtime.messages') IS NULL THEN
    RAISE NOTICE 'realtime.messages is absent; provider policy DDL skipped in this target.';
    RETURN;
  END IF;
  IF to_regprocedure('auth.uid()') IS NULL
     OR to_regprocedure('auth.jwt()') IS NULL
     OR to_regprocedure('realtime.topic()') IS NULL THEN
    RAISE EXCEPTION 'provider Realtime helpers auth.uid(), auth.jwt(), and realtime.topic() are required';
  END IF;

  EXECUTE 'DROP POLICY IF EXISTS realtime_messages_select_private_channels ON realtime.messages';
  EXECUTE 'DROP POLICY IF EXISTS realtime_messages_insert_private_channels ON realtime.messages';
  EXECUTE 'DROP POLICY IF EXISTS realtime_messages_select_scoped_private_channels ON realtime.messages';
  EXECUTE $sql$
    CREATE POLICY realtime_messages_select_scoped_private_channels
    ON realtime.messages
    FOR SELECT TO authenticated
    USING (
      topic = (SELECT realtime.topic())
      AND extension IN ('broadcast', 'presence')
      AND nexora.realtime_current_channel_authorized(topic)
    )
  $sql$;
END
$realtime_policy$;

COMMIT;
