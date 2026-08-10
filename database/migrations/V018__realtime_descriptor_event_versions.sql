-- M3-DB01 forward-only hardening: a scoped descriptor must carry the exact
-- version frozen for its event route. A positive integer alone is not authority.
BEGIN;
SET LOCAL ROLE nexora_migrator;

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
  expected_event_version bigint;
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
  expected_event_version := CASE WHEN expected_event_type IS NOT NULL THEN 1 ELSE NULL END;

  IF expected_event_type IS NULL
     OR claims ->> 'nexora_realtime_event_type' IS DISTINCT FROM expected_event_type
     OR claims ->> 'nexora_realtime_event_version' IS DISTINCT FROM expected_event_version::text THEN
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

REVOKE ALL ON FUNCTION nexora.realtime_current_channel_authorized(text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION nexora.realtime_current_channel_authorized(text) TO nexora_runtime;

DO $grant_authenticated$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
    GRANT EXECUTE ON FUNCTION nexora.realtime_current_channel_authorized(text) TO authenticated;
  END IF;
END
$grant_authenticated$;

COMMIT;
