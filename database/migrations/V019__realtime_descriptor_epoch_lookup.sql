-- M3-DB01 forward repair: M3-T03 must issue a descriptor whose epoch exactly
-- matches the private epoch projection, without granting the runtime role a
-- readable epoch table. The caller supplies only a server-derived topic; the
-- subject and selected tenant context come from the already-authorized
-- transaction-local runtime boundary and are revalidated below.
BEGIN;
SET LOCAL ROLE nexora_migrator;

CREATE FUNCTION nexora.current_realtime_descriptor_epoch(in_topic text)
RETURNS bigint
LANGUAGE plpgsql
STABLE
STRICT
SECURITY DEFINER
SET search_path = pg_catalog, nexora
AS $function$
DECLARE
  current_subject uuid;
  current_organization uuid;
  current_membership uuid;
  current_epoch bigint;
  parts text[];
  valid_route boolean;
BEGIN
  -- nexora_runtime is the already-approved application-server trust boundary:
  -- its transaction-local identity is established only after Spring validates
  -- the bearer and re-reads the active membership. It is not browser or Data
  -- API authority. A caller able to execute arbitrary runtime SQL can already
  -- forge the same RLS context and is outside this scalar function's boundary.
  current_subject := nullif(current_setting('nexora.subject_id', true), '')::uuid;
  current_organization := nullif(current_setting('nexora.organization_id', true), '')::uuid;
  current_membership := nullif(current_setting('nexora.membership_id', true), '')::uuid;
  parts := string_to_array(in_topic, ':');
  valid_route := array_length(parts, 1) = 3 AND (
    (parts[1] = 'tenant' AND parts[3] IN ('publication', 'workflow'))
    OR (parts[1] = 'resource' AND parts[3] IN ('job-progress', 'presence'))
  );

  IF current_subject IS NULL
     OR current_organization IS NULL
     OR current_membership IS NULL
     OR NOT valid_route
     OR NOT EXISTS (
       SELECT 1
       FROM nexora.membership_authorizations AS actor
       WHERE actor.membership_id = current_membership
         AND actor.subject_id = current_subject
         AND actor.organization_id = current_organization
         AND actor.status = 'ACTIVE'
     )
     OR (parts[1] = 'tenant' AND parts[2] IS DISTINCT FROM current_organization::text)
     OR (parts[1] = 'resource' AND parts[3] = 'job-progress' AND NOT EXISTS (
       SELECT 1
       FROM nexora.outbox_events AS event
       WHERE event.topic = in_topic
         AND event.resource_id = parts[2]::uuid
         AND event.organization_id = current_organization
         AND event.subject_id = current_subject
     ))
     OR (parts[1] = 'resource' AND parts[3] = 'presence' AND NOT EXISTS (
       SELECT 1
       FROM nexora.realtime_presence_resources AS resource
       WHERE resource.resource_id = parts[2]::uuid
         AND resource.organization_id = current_organization
     ))
     OR NOT nexora.realtime_private_channel_authorized(in_topic, current_subject) THEN
    RETURN NULL;
  END IF;

  SELECT epoch.authorization_epoch
  INTO current_epoch
  FROM nexora.realtime_authorization_epochs AS epoch
  WHERE epoch.subject_id = current_subject;

  RETURN current_epoch;
EXCEPTION
  WHEN invalid_text_representation OR numeric_value_out_of_range THEN
    RETURN NULL;
END
$function$;

REVOKE ALL ON FUNCTION nexora.current_realtime_descriptor_epoch(text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION nexora.current_realtime_descriptor_epoch(text) TO nexora_runtime;

COMMIT;
