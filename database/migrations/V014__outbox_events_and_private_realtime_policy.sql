-- M3-DB01 draft: transactional outbox, bounded publisher leases, and only the
-- provider-documented policies on realtime.messages. This migration creates
-- no managed-schema table, function, index, or trigger.

BEGIN;
SET LOCAL ROLE nexora_migrator;

CREATE TYPE nexora.outbox_state AS ENUM (
  'PENDING',
  'CLAIMED',
  'PUBLISHED',
  'FAILED',
  'DEAD_LETTER'
);

CREATE TYPE nexora.outbox_event_type AS ENUM (
  'PUBLICATION_INVALIDATED',
  'WORKFLOW_TRANSITIONED',
  'JOB_PROGRESS_CHANGED',
  'NOTIFICATION_ENQUEUED',
  'PRESENCE_CHANGED',
  'OUTBOX_RECORDED'
);

CREATE TABLE nexora.outbox_events (
  id uuid PRIMARY KEY,
  organization_id uuid NOT NULL,
  subject_id uuid NOT NULL,
  actor_id uuid NOT NULL,
  resource_type text NOT NULL CHECK (resource_type ~ '^[a-z][a-z0-9_-]{0,63}$'),
  resource_id uuid NOT NULL,
  event_type nexora.outbox_event_type NOT NULL,
  event_version bigint NOT NULL CHECK (event_version > 0),
  topic text NOT NULL CHECK (char_length(topic) BETWEEN 1 AND 180),
  schema_version text NOT NULL CHECK (schema_version ~ '^[0-9]+\.[0-9]+\.[0-9]+$'),
  idempotency_key_digest text NOT NULL CHECK (char_length(idempotency_key_digest) BETWEEN 16 AND 160),
  request_fingerprint_digest text NOT NULL CHECK (char_length(request_fingerprint_digest) BETWEEN 16 AND 160),
  payload_digest text NOT NULL CHECK (char_length(payload_digest) BETWEEN 16 AND 160),
  safe_payload jsonb NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(safe_payload) = 'object'),
  state nexora.outbox_state NOT NULL DEFAULT 'PENDING',
  claim_owner text CHECK (claim_owner IS NULL OR claim_owner ~ '^[A-Za-z0-9][A-Za-z0-9._:-]{0,127}$'),
  claim_expires_at timestamptz,
  attempt_count integer NOT NULL DEFAULT 0 CHECK (attempt_count BETWEEN 0 AND 5),
  last_error_code text CHECK (last_error_code IS NULL OR last_error_code ~ '^[A-Z][A-Z0-9_]{1,63}$'),
  available_at timestamptz NOT NULL DEFAULT transaction_timestamp(),
  published_at timestamptz,
  failed_at timestamptz,
  dead_letter_at timestamptz,
  retain_until timestamptz,
  occurred_at timestamptz NOT NULL DEFAULT transaction_timestamp(),
  created_at timestamptz NOT NULL DEFAULT transaction_timestamp(),
  updated_at timestamptz NOT NULL DEFAULT transaction_timestamp(),
  CONSTRAINT outbox_events_claim_shape_check CHECK (
    (state = 'CLAIMED') = (claim_owner IS NOT NULL AND claim_expires_at IS NOT NULL)
  ),
  CONSTRAINT outbox_events_published_shape_check CHECK (
    state <> 'PUBLISHED'
    OR (published_at IS NOT NULL AND retain_until > published_at)
  ),
  CONSTRAINT outbox_events_failed_shape_check CHECK (
    state <> 'FAILED'
    OR (failed_at IS NOT NULL AND last_error_code IS NOT NULL)
  ),
  CONSTRAINT outbox_events_dead_letter_shape_check CHECK (
    state <> 'DEAD_LETTER'
    OR (
      failed_at IS NOT NULL
      AND dead_letter_at IS NOT NULL
      AND last_error_code IS NOT NULL
      AND retain_until > dead_letter_at
    )
  ),
  CONSTRAINT outbox_events_retention_shape_check CHECK (
    (state IN ('PUBLISHED', 'DEAD_LETTER')) = (retain_until IS NOT NULL)
  )
);

ALTER TABLE nexora.outbox_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE nexora.outbox_events FORCE ROW LEVEL SECURITY;

CREATE POLICY outbox_events_migrator_control
ON nexora.outbox_events
FOR ALL
TO nexora_migrator
USING (true)
WITH CHECK (true);

-- The no-login migration owner is the execution identity of the tightly
-- scoped SECURITY DEFINER functions below. FORCE RLS still applies, so grant
-- it a permanent read-only policy on the private current-membership projection.
CREATE POLICY membership_authorizations_m3_runtime_evidence
ON nexora.membership_authorizations
FOR SELECT
TO nexora_migrator
USING (true);

CREATE UNIQUE INDEX outbox_events_idempotency_scope_uk
ON nexora.outbox_events (organization_id, topic, event_type, idempotency_key_digest);

CREATE INDEX outbox_events_ready_lookup_idx
ON nexora.outbox_events (available_at, created_at, id)
WHERE state IN ('PENDING', 'FAILED');

CREATE INDEX outbox_events_expired_claim_lookup_idx
ON nexora.outbox_events (claim_expires_at, created_at, id)
WHERE state = 'CLAIMED';

CREATE INDEX outbox_events_terminal_lookup_idx
ON nexora.outbox_events (retain_until, updated_at, id)
WHERE state IN ('PUBLISHED', 'DEAD_LETTER');

CREATE INDEX outbox_events_topic_lookup_idx
ON nexora.outbox_events (organization_id, topic, occurred_at, id);

CREATE INDEX outbox_events_resource_lookup_idx
ON nexora.outbox_events (organization_id, resource_type, resource_id, occurred_at, id);

COMMENT ON TABLE nexora.outbox_events IS
  'Application-owned transactional outbox. Runtime has function-only access; publisher leases are bounded and terminal rows retain operator-visible evidence.';
COMMENT ON COLUMN nexora.outbox_events.retain_until IS
  'Earliest reviewed purge eligibility. V014 grants no DELETE and implements no purge job.';

CREATE FUNCTION nexora.outbox_safe_payload_is_allowed(payload jsonb)
RETURNS boolean
LANGUAGE plpgsql
IMMUTABLE
STRICT
SECURITY INVOKER
SET search_path = pg_catalog, nexora
AS $function$
DECLARE
  key text;
  value jsonb;
  value_text text;
  safe_display jsonb;
  forbidden_value_pattern constant text := '(authorization|bearer|token|secret|password|cookie|provider|prompt|private[ _-]?key|access[ _-]?token|api[ _-]?key|pii|email|phone|body|raw|html|document)';
BEGIN
  IF jsonb_typeof(payload) <> 'object' THEN
    RETURN false;
  END IF;

  FOR key, value IN SELECT * FROM jsonb_each(payload) LOOP
    IF key NOT IN (
      'resourceId', 'resourceType', 'organizationId', 'subjectId', 'actorId',
      'eventVersion', 'jobState', 'progress', 'correlationId', 'traceId',
      'receiptId', 'schemaVersion', 'safeDisplay'
    ) THEN
      RETURN false;
    END IF;

    IF key = 'safeDisplay' THEN
      CONTINUE;
    ELSIF key = 'progress' THEN
      IF jsonb_typeof(value) <> 'number'
         OR (value #>> '{}')::numeric < 0
         OR (value #>> '{}')::numeric > 100 THEN
        RETURN false;
      END IF;
    ELSIF key = 'eventVersion' THEN
      IF jsonb_typeof(value) <> 'number'
         OR (value #>> '{}')::numeric < 1
         OR (value #>> '{}')::numeric <> trunc((value #>> '{}')::numeric) THEN
        RETURN false;
      END IF;
    ELSE
      IF jsonb_typeof(value) <> 'string' THEN
        RETURN false;
      END IF;
      value_text := value #>> '{}';
      IF char_length(value_text) NOT BETWEEN 1 AND 160
         OR value_text !~ '^[[:print:]]*$'
         OR value_text ~* forbidden_value_pattern THEN
        RETURN false;
      END IF;
    END IF;
  END LOOP;

  IF payload ? 'safeDisplay' THEN
    safe_display := payload -> 'safeDisplay';
    IF jsonb_typeof(safe_display) <> 'object'
       OR NOT (safe_display ? 'label')
       OR NOT (safe_display ? 'status') THEN
      RETURN false;
    END IF;
    FOR key, value IN SELECT * FROM jsonb_each(safe_display) LOOP
      IF key NOT IN ('label', 'status', 'hint', 'state', 'variant', 'progressText')
         OR jsonb_typeof(value) <> 'string' THEN
        RETURN false;
      END IF;
      value_text := value #>> '{}';
      IF value_text !~ '^[[:print:]]*$'
         OR value_text ~* forbidden_value_pattern THEN
        RETURN false;
      END IF;

      IF key = 'label' AND char_length(value_text) NOT BETWEEN 1 AND 80 THEN
        RETURN false;
      ELSIF key = 'status' AND char_length(value_text) NOT BETWEEN 1 AND 32 THEN
        RETURN false;
      ELSIF key = 'hint' AND char_length(value_text) > 120 THEN
        RETURN false;
      ELSIF key = 'state' AND char_length(value_text) NOT BETWEEN 1 AND 32 THEN
        RETURN false;
      ELSIF key = 'variant' AND value_text NOT IN ('neutral', 'success', 'warning', 'danger', 'info') THEN
        RETURN false;
      ELSIF key = 'progressText' AND char_length(value_text) > 40 THEN
        RETURN false;
      END IF;
    END LOOP;
  END IF;

  RETURN true;
EXCEPTION
  WHEN invalid_text_representation OR numeric_value_out_of_range THEN
    RETURN false;
END
$function$;

CREATE FUNCTION nexora.outbox_topic_is_valid(
  in_topic text,
  in_organization_id uuid,
  in_resource_id uuid,
  in_event_type nexora.outbox_event_type
)
RETURNS boolean
LANGUAGE plpgsql
IMMUTABLE
STRICT
SECURITY INVOKER
SET search_path = pg_catalog, nexora
AS $function$
DECLARE
  parts text[];
  owner_id uuid;
BEGIN
  parts := string_to_array(in_topic, ':');
  IF array_length(parts, 1) <> 3
     OR parts[1] NOT IN ('tenant', 'resource', 'job')
     OR parts[2] !~* '^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$'
     OR parts[3] NOT IN ('publication', 'workflow', 'job-progress', 'notification', 'presence', 'outbox') THEN
    RETURN false;
  END IF;

  owner_id := parts[2]::uuid;
  RETURN CASE in_event_type
    WHEN 'PUBLICATION_INVALIDATED' THEN parts[1] = 'tenant' AND parts[3] = 'publication' AND owner_id = in_organization_id
    WHEN 'WORKFLOW_TRANSITIONED' THEN parts[1] = 'tenant' AND parts[3] = 'workflow' AND owner_id = in_organization_id
    WHEN 'JOB_PROGRESS_CHANGED' THEN parts[1] = 'resource' AND parts[3] = 'job-progress' AND owner_id = in_resource_id
    WHEN 'NOTIFICATION_ENQUEUED' THEN parts[1] = 'tenant' AND parts[3] = 'notification' AND owner_id = in_organization_id
    WHEN 'PRESENCE_CHANGED' THEN parts[1] = 'resource' AND parts[3] = 'presence' AND owner_id = in_resource_id
    WHEN 'OUTBOX_RECORDED' THEN parts[1] = 'tenant' AND parts[3] = 'outbox' AND owner_id = in_organization_id
    ELSE false
  END;
END
$function$;

CREATE FUNCTION nexora.guard_outbox_event_mutation()
RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = pg_catalog, nexora
AS $function$
BEGIN
  IF TG_OP = 'INSERT' THEN
    IF NEW.state <> 'PENDING'
       OR NEW.attempt_count <> 0
       OR NEW.claim_owner IS NOT NULL
       OR NEW.claim_expires_at IS NOT NULL
       OR NEW.published_at IS NOT NULL
       OR NEW.failed_at IS NOT NULL
       OR NEW.dead_letter_at IS NOT NULL
       OR NEW.retain_until IS NOT NULL
       OR NEW.last_error_code IS NOT NULL
       OR NOT nexora.outbox_safe_payload_is_allowed(NEW.safe_payload)
       OR NOT nexora.outbox_topic_is_valid(NEW.topic, NEW.organization_id, NEW.resource_id, NEW.event_type) THEN
      RAISE EXCEPTION USING ERRCODE = '22023', MESSAGE = 'OUTBOX_INSERT_REJECTED';
    END IF;
    RETURN NEW;
  END IF;

  IF ROW(
    NEW.id, NEW.organization_id, NEW.subject_id, NEW.actor_id,
    NEW.resource_type, NEW.resource_id, NEW.event_type, NEW.event_version,
    NEW.topic, NEW.schema_version, NEW.idempotency_key_digest,
    NEW.request_fingerprint_digest, NEW.payload_digest, NEW.safe_payload,
    NEW.occurred_at, NEW.created_at
  ) IS DISTINCT FROM ROW(
    OLD.id, OLD.organization_id, OLD.subject_id, OLD.actor_id,
    OLD.resource_type, OLD.resource_id, OLD.event_type, OLD.event_version,
    OLD.topic, OLD.schema_version, OLD.idempotency_key_digest,
    OLD.request_fingerprint_digest, OLD.payload_digest, OLD.safe_payload,
    OLD.occurred_at, OLD.created_at
  ) THEN
    RAISE EXCEPTION USING ERRCODE = '55000', MESSAGE = 'OUTBOX_ENVELOPE_IMMUTABLE';
  END IF;

  IF NEW.updated_at IS DISTINCT FROM transaction_timestamp() THEN
    RAISE EXCEPTION USING ERRCODE = '55000', MESSAGE = 'OUTBOX_TIMESTAMP_REJECTED';
  END IF;

  IF OLD.state IN ('PENDING', 'FAILED') AND NEW.state = 'CLAIMED' THEN
    IF NEW.attempt_count <> OLD.attempt_count + 1 OR NEW.claim_expires_at <= clock_timestamp() THEN
      RAISE EXCEPTION USING ERRCODE = '55000', MESSAGE = 'OUTBOX_CLAIM_REJECTED';
    END IF;
  ELSIF OLD.state = 'CLAIMED' AND NEW.state = 'CLAIMED' THEN
    IF OLD.claim_expires_at > clock_timestamp()
       OR NEW.attempt_count <> OLD.attempt_count + 1
       OR NEW.claim_expires_at <= clock_timestamp() THEN
      RAISE EXCEPTION USING ERRCODE = '55000', MESSAGE = 'OUTBOX_RECLAIM_REJECTED';
    END IF;
  ELSIF OLD.state = 'CLAIMED' AND NEW.state IN ('PUBLISHED', 'FAILED', 'DEAD_LETTER') THEN
    IF NEW.attempt_count <> OLD.attempt_count THEN
      RAISE EXCEPTION USING ERRCODE = '55000', MESSAGE = 'OUTBOX_TERMINAL_REJECTED';
    END IF;
  ELSIF OLD.state = 'FAILED' AND NEW.state = 'DEAD_LETTER' THEN
    IF NEW.attempt_count <> OLD.attempt_count
       OR NEW.claim_owner IS NOT NULL
       OR NEW.claim_expires_at IS NOT NULL
       OR NEW.failed_at IS NULL
       OR NEW.dead_letter_at IS NULL
       OR NEW.retain_until <= NEW.dead_letter_at
       OR NEW.last_error_code IS NULL THEN
      RAISE EXCEPTION USING ERRCODE = '55000', MESSAGE = 'OUTBOX_TERMINAL_REJECTED';
    END IF;
  ELSE
    RAISE EXCEPTION USING ERRCODE = '55000', MESSAGE = 'OUTBOX_TRANSITION_REJECTED';
  END IF;

  RETURN NEW;
END
$function$;

CREATE TRIGGER outbox_events_guard_mutation
BEFORE INSERT OR UPDATE ON nexora.outbox_events
FOR EACH ROW
EXECUTE FUNCTION nexora.guard_outbox_event_mutation();

CREATE FUNCTION nexora.record_outbox_event(
  in_id uuid,
  in_organization_id uuid,
  in_subject_id uuid,
  in_actor_id uuid,
  in_resource_type text,
  in_resource_id uuid,
  in_event_type nexora.outbox_event_type,
  in_event_version bigint,
  in_topic text,
  in_schema_version text,
  in_idempotency_key_digest text,
  in_request_fingerprint_digest text,
  in_payload_digest text,
  in_safe_payload jsonb,
  in_occurred_at timestamptz DEFAULT transaction_timestamp()
)
RETURNS uuid
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, nexora
AS $function$
DECLARE
  current_organization uuid;
  current_subject uuid;
  current_membership uuid;
  existing nexora.outbox_events%ROWTYPE;
BEGIN
  current_organization := NULLIF(current_setting('nexora.organization_id', true), '')::uuid;
  current_subject := NULLIF(current_setting('nexora.subject_id', true), '')::uuid;
  current_membership := NULLIF(current_setting('nexora.membership_id', true), '')::uuid;

  IF current_organization IS NULL OR current_subject IS NULL OR current_membership IS NULL
     OR in_organization_id IS DISTINCT FROM current_organization
     OR in_subject_id IS DISTINCT FROM current_subject
     OR in_actor_id IS DISTINCT FROM current_subject
     OR NOT EXISTS (
       SELECT 1 FROM nexora.membership_authorizations AS actor
       WHERE actor.organization_id = current_organization
         AND actor.subject_id = current_subject
         AND actor.membership_id = current_membership
         AND actor.status = 'ACTIVE'
     ) THEN
    RAISE EXCEPTION USING ERRCODE = '42501', MESSAGE = 'UNOWNED_SUBJECT';
  END IF;

  IF in_schema_version <> '1.0.0'
     OR in_event_version <= 0
     OR in_resource_type !~ '^[a-z][a-z0-9_-]{0,63}$'
     OR NOT nexora.outbox_topic_is_valid(in_topic, in_organization_id, in_resource_id, in_event_type)
     OR NOT nexora.outbox_safe_payload_is_allowed(in_safe_payload) THEN
    RAISE EXCEPTION USING ERRCODE = '22023', MESSAGE = 'PAYLOAD_REJECTED';
  END IF;

  IF (in_safe_payload ? 'organizationId' AND in_safe_payload ->> 'organizationId' <> in_organization_id::text)
     OR (in_safe_payload ? 'subjectId' AND in_safe_payload ->> 'subjectId' <> in_subject_id::text)
     OR (in_safe_payload ? 'actorId' AND in_safe_payload ->> 'actorId' <> in_actor_id::text)
     OR (in_safe_payload ? 'resourceId' AND in_safe_payload ->> 'resourceId' <> in_resource_id::text)
     OR (in_safe_payload ? 'resourceType' AND in_safe_payload ->> 'resourceType' <> in_resource_type)
     OR (in_safe_payload ? 'eventVersion' AND (in_safe_payload ->> 'eventVersion')::bigint <> in_event_version)
     OR (in_safe_payload ? 'schemaVersion' AND in_safe_payload ->> 'schemaVersion' <> in_schema_version) THEN
    RAISE EXCEPTION USING ERRCODE = '22023', MESSAGE = 'PAYLOAD_REJECTED';
  END IF;

  INSERT INTO nexora.outbox_events (
    id, organization_id, subject_id, actor_id, resource_type, resource_id,
    event_type, event_version, topic, schema_version,
    idempotency_key_digest, request_fingerprint_digest, payload_digest,
    safe_payload, occurred_at
  ) VALUES (
    in_id, in_organization_id, in_subject_id, in_actor_id, in_resource_type,
    in_resource_id, in_event_type, in_event_version, in_topic,
    in_schema_version, in_idempotency_key_digest,
    in_request_fingerprint_digest, in_payload_digest, in_safe_payload,
    in_occurred_at
  )
  ON CONFLICT (organization_id, topic, event_type, idempotency_key_digest)
  DO NOTHING;

  IF FOUND THEN
    RETURN in_id;
  END IF;

  SELECT * INTO existing
  FROM nexora.outbox_events
  WHERE organization_id = in_organization_id
    AND topic = in_topic
    AND event_type = in_event_type
    AND idempotency_key_digest = in_idempotency_key_digest;

  IF NOT FOUND
     OR existing.request_fingerprint_digest IS DISTINCT FROM in_request_fingerprint_digest
     OR existing.payload_digest IS DISTINCT FROM in_payload_digest
     OR existing.subject_id IS DISTINCT FROM in_subject_id
     OR existing.resource_id IS DISTINCT FROM in_resource_id
     OR existing.event_version IS DISTINCT FROM in_event_version THEN
    RAISE EXCEPTION USING ERRCODE = '23505', MESSAGE = 'IDEMPOTENCY_KEY_REUSED';
  END IF;

  RETURN existing.id;
END
$function$;

CREATE FUNCTION nexora.claim_outbox_events(
  in_claim_owner text,
  in_claim_lease interval,
  in_batch_size integer DEFAULT 1
)
RETURNS SETOF nexora.outbox_events
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, nexora
AS $function$
BEGIN
  IF in_claim_owner !~ '^[A-Za-z0-9][A-Za-z0-9._:-]{0,127}$'
     OR in_claim_lease < interval '1 second'
     OR in_claim_lease > interval '15 minutes'
     OR in_batch_size NOT BETWEEN 1 AND 100 THEN
    RAISE EXCEPTION USING ERRCODE = '22023', MESSAGE = 'OUTBOX_CLAIM_REJECTED';
  END IF;

  UPDATE nexora.outbox_events
  SET state = 'DEAD_LETTER',
      claim_owner = NULL,
      claim_expires_at = NULL,
      failed_at = clock_timestamp(),
      dead_letter_at = clock_timestamp(),
      last_error_code = 'CLAIM_LEASE_EXHAUSTED',
      retain_until = clock_timestamp() + interval '90 days',
      updated_at = transaction_timestamp()
  WHERE state = 'CLAIMED'
    AND claim_expires_at <= clock_timestamp()
    AND attempt_count >= 5;

  RETURN QUERY
  WITH claim_candidates AS (
    SELECT outbox.id
    FROM nexora.outbox_events AS outbox
    WHERE outbox.attempt_count < 5
      AND (
        (outbox.state IN ('PENDING', 'FAILED') AND outbox.available_at <= clock_timestamp())
        OR (outbox.state = 'CLAIMED' AND outbox.claim_expires_at <= clock_timestamp())
      )
    ORDER BY outbox.available_at, outbox.created_at, outbox.id
    LIMIT in_batch_size
    FOR UPDATE SKIP LOCKED
  )
  UPDATE nexora.outbox_events AS outbox
  SET state = 'CLAIMED',
      claim_owner = in_claim_owner,
      claim_expires_at = clock_timestamp() + in_claim_lease,
      attempt_count = outbox.attempt_count + 1,
      updated_at = transaction_timestamp()
  FROM claim_candidates
  WHERE outbox.id = claim_candidates.id
  RETURNING outbox.*;
END
$function$;

CREATE FUNCTION nexora.publish_claimed_outbox_event(in_event_id uuid, in_claim_owner text)
RETURNS nexora.outbox_events
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, nexora
AS $function$
DECLARE
  updated_row nexora.outbox_events;
BEGIN
  UPDATE nexora.outbox_events
  SET state = 'PUBLISHED', claim_owner = NULL, claim_expires_at = NULL,
      published_at = clock_timestamp(), last_error_code = NULL,
      retain_until = clock_timestamp() + interval '30 days',
      updated_at = transaction_timestamp()
  WHERE id = in_event_id
    AND state = 'CLAIMED'
    AND claim_owner = in_claim_owner
    AND claim_expires_at > clock_timestamp()
  RETURNING * INTO updated_row;

  IF NOT FOUND THEN
    RAISE EXCEPTION USING ERRCODE = '55000', MESSAGE = 'OUTBOX_TERMINAL_FAILURE';
  END IF;
  RETURN updated_row;
END
$function$;

CREATE FUNCTION nexora.fail_claimed_outbox_event(
  in_event_id uuid,
  in_claim_owner text,
  in_error_code text,
  in_force_dead_letter boolean DEFAULT false,
  in_retry_backoff interval DEFAULT interval '5 minutes'
)
RETURNS nexora.outbox_events
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, nexora
AS $function$
DECLARE
  updated_row nexora.outbox_events;
BEGIN
  IF in_error_code !~ '^[A-Z][A-Z0-9_]{1,63}$'
     OR in_retry_backoff < interval '1 second'
     OR in_retry_backoff > interval '24 hours' THEN
    RAISE EXCEPTION USING ERRCODE = '22023', MESSAGE = 'OUTBOX_TERMINAL_FAILURE';
  END IF;

  UPDATE nexora.outbox_events
  SET state = CASE WHEN in_force_dead_letter OR attempt_count >= 5 THEN 'DEAD_LETTER'::nexora.outbox_state ELSE 'FAILED'::nexora.outbox_state END,
      claim_owner = NULL,
      claim_expires_at = NULL,
      failed_at = clock_timestamp(),
      dead_letter_at = CASE WHEN in_force_dead_letter OR attempt_count >= 5 THEN clock_timestamp() ELSE NULL END,
      last_error_code = in_error_code,
      available_at = CASE WHEN in_force_dead_letter OR attempt_count >= 5 THEN available_at ELSE clock_timestamp() + in_retry_backoff END,
      retain_until = CASE WHEN in_force_dead_letter OR attempt_count >= 5 THEN clock_timestamp() + interval '90 days' ELSE NULL END,
      updated_at = transaction_timestamp()
  WHERE id = in_event_id
    AND state = 'CLAIMED'
    AND claim_owner = in_claim_owner
    AND claim_expires_at > clock_timestamp()
  RETURNING * INTO updated_row;

  IF NOT FOUND THEN
    RAISE EXCEPTION USING ERRCODE = '55000', MESSAGE = 'OUTBOX_TERMINAL_FAILURE';
  END IF;
  RETURN updated_row;
END
$function$;

CREATE FUNCTION nexora.realtime_private_channel_authorized(in_topic text, in_subject_id uuid)
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
     OR parts[1] NOT IN ('tenant', 'resource', 'job')
     OR parts[2] !~* '^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$'
     OR parts[3] NOT IN ('publication', 'workflow', 'job-progress', 'notification', 'presence', 'outbox') THEN
    RETURN false;
  END IF;
  owner_id := parts[2]::uuid;

  IF parts[1] = 'tenant' THEN
    RETURN EXISTS (
      SELECT 1 FROM nexora.membership_authorizations AS actor
      WHERE actor.organization_id = owner_id
        AND actor.subject_id = in_subject_id
        AND actor.status = 'ACTIVE'
    );
  END IF;

  RETURN EXISTS (
    SELECT 1
    FROM nexora.outbox_events AS event
    JOIN nexora.membership_authorizations AS actor
      ON actor.organization_id = event.organization_id
     AND actor.subject_id = in_subject_id
     AND actor.status = 'ACTIVE'
    WHERE event.topic = in_topic
      AND event.resource_id = owner_id
  );
END
$function$;

CREATE FUNCTION nexora.realtime_current_channel_authorized(in_topic text)
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
BEGIN
  current_topic := NULLIF(current_setting('realtime.topic', true), '');
  current_subject := NULLIF(current_setting('request.jwt.claim.sub', true), '')::uuid;

  IF current_topic IS NULL
     OR current_subject IS NULL
     OR in_topic IS DISTINCT FROM current_topic THEN
    RETURN false;
  END IF;

  RETURN nexora.realtime_private_channel_authorized(in_topic, current_subject);
EXCEPTION
  WHEN invalid_text_representation THEN
    RETURN false;
END
$function$;

REVOKE ALL ON TABLE nexora.outbox_events FROM PUBLIC;
REVOKE ALL ON FUNCTION nexora.outbox_safe_payload_is_allowed(jsonb) FROM PUBLIC;
REVOKE ALL ON FUNCTION nexora.outbox_topic_is_valid(text, uuid, uuid, nexora.outbox_event_type) FROM PUBLIC;
REVOKE ALL ON FUNCTION nexora.guard_outbox_event_mutation() FROM PUBLIC;
REVOKE ALL ON FUNCTION nexora.record_outbox_event(uuid, uuid, uuid, uuid, text, uuid, nexora.outbox_event_type, bigint, text, text, text, text, text, jsonb, timestamptz) FROM PUBLIC;
REVOKE ALL ON FUNCTION nexora.claim_outbox_events(text, interval, integer) FROM PUBLIC;
REVOKE ALL ON FUNCTION nexora.publish_claimed_outbox_event(uuid, text) FROM PUBLIC;
REVOKE ALL ON FUNCTION nexora.fail_claimed_outbox_event(uuid, text, text, boolean, interval) FROM PUBLIC;
REVOKE ALL ON FUNCTION nexora.realtime_private_channel_authorized(text, uuid) FROM PUBLIC;
REVOKE ALL ON FUNCTION nexora.realtime_current_channel_authorized(text) FROM PUBLIC;

GRANT USAGE ON TYPE nexora.outbox_state, nexora.outbox_event_type TO nexora_runtime;
GRANT EXECUTE ON FUNCTION nexora.outbox_safe_payload_is_allowed(jsonb) TO nexora_runtime;
GRANT EXECUTE ON FUNCTION nexora.record_outbox_event(uuid, uuid, uuid, uuid, text, uuid, nexora.outbox_event_type, bigint, text, text, text, text, text, jsonb, timestamptz) TO nexora_runtime;
GRANT EXECUTE ON FUNCTION nexora.claim_outbox_events(text, interval, integer) TO nexora_runtime;
GRANT EXECUTE ON FUNCTION nexora.publish_claimed_outbox_event(uuid, text) TO nexora_runtime;
GRANT EXECUTE ON FUNCTION nexora.fail_claimed_outbox_event(uuid, text, text, boolean, interval) TO nexora_runtime;
GRANT EXECUTE ON FUNCTION nexora.realtime_private_channel_authorized(text, uuid) TO nexora_runtime;
GRANT EXECUTE ON FUNCTION nexora.realtime_current_channel_authorized(text) TO nexora_runtime;

DO $grant_authenticated$
DECLARE
  api_role text;
BEGIN
  FOREACH api_role IN ARRAY ARRAY['anon', 'authenticated', 'service_role'] LOOP
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = api_role) THEN
      EXECUTE format('REVOKE ALL ON TABLE nexora.outbox_events FROM %I', api_role);
    END IF;
  END LOOP;

  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
    GRANT EXECUTE ON FUNCTION nexora.realtime_current_channel_authorized(text) TO authenticated;
  END IF;
END
$grant_authenticated$;

-- The application migration role owns only application objects. Provider
-- policy DDL must execute as the migration session identity that Supabase gives
-- policy authority on realtime.messages; no other managed object is touched.
RESET ROLE;

DO $realtime_policy$
BEGIN
  IF to_regclass('realtime.messages') IS NULL THEN
    RAISE NOTICE 'realtime.messages is absent; provider policy DDL skipped in this target.';
    RETURN;
  END IF;
  IF to_regprocedure('auth.uid()') IS NULL OR to_regprocedure('realtime.topic()') IS NULL THEN
    RAISE EXCEPTION 'provider Realtime helpers auth.uid() and realtime.topic() are required';
  END IF;

  EXECUTE 'DROP POLICY IF EXISTS realtime_messages_select_private_channels ON realtime.messages';
  EXECUTE 'DROP POLICY IF EXISTS realtime_messages_insert_private_channels ON realtime.messages';
  EXECUTE $sql$
    CREATE POLICY realtime_messages_select_private_channels
    ON realtime.messages
    FOR SELECT TO authenticated
    USING (
      topic = (SELECT realtime.topic())
      AND nexora.realtime_current_channel_authorized(topic)
    )
  $sql$;
  EXECUTE $sql$
    CREATE POLICY realtime_messages_insert_private_channels
    ON realtime.messages
    FOR INSERT TO authenticated
    WITH CHECK (
      topic = (SELECT realtime.topic())
      AND nexora.realtime_current_channel_authorized(topic)
    )
  $sql$;
END
$realtime_policy$;

SET LOCAL ROLE nexora_migrator;

COMMIT;
