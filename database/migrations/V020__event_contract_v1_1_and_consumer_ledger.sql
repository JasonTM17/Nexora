-- M3-DB01 forward-only alignment with the frozen M3-T01 event contract 1.1.0.
--
-- V014 accepted the former 1.0.0 payload shape.  That shape has a free-form
-- safeDisplay boundary and therefore cannot be translated into 1.1.0.  This
-- migration preserves historical terminal evidence, dead-letters outstanding
-- legacy rows with an explicit reason, and permits only 1.1.0 on the runtime
-- record path.  It also creates the private durable event ledger consumed by
-- M3-T05; it does not implement the consumer or canonical-JCS verification.

BEGIN;
SET LOCAL ROLE nexora_migrator;

CREATE FUNCTION nexora.outbox_event_route_v1_1_is_valid(
  in_event_type nexora.outbox_event_type,
  in_resource_type text,
  in_topic text,
  in_organization_id uuid,
  in_resource_id uuid
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
     OR parts[1] NOT IN ('tenant', 'resource')
     OR parts[2] !~ '^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$'
     OR parts[3] !~ '^[a-z][a-z0-9-]{0,31}$' THEN
    RETURN false;
  END IF;

  owner_id := parts[2]::uuid;
  RETURN CASE in_event_type
    WHEN 'PUBLICATION_INVALIDATED' THEN in_resource_type = 'page'
      AND parts[1] = 'tenant' AND parts[3] = 'publication' AND owner_id = in_organization_id
    WHEN 'WORKFLOW_TRANSITIONED' THEN in_resource_type = 'page'
      AND parts[1] = 'tenant' AND parts[3] = 'workflow' AND owner_id = in_organization_id
    WHEN 'JOB_PROGRESS_CHANGED' THEN in_resource_type = 'job'
      AND parts[1] = 'resource' AND parts[3] = 'job-progress' AND owner_id = in_resource_id
    WHEN 'NOTIFICATION_ENQUEUED' THEN in_resource_type = 'notification'
      AND parts[1] = 'tenant' AND parts[3] = 'notification' AND owner_id = in_organization_id
    WHEN 'PRESENCE_CHANGED' THEN in_resource_type = 'collaboration_session'
      AND parts[1] = 'resource' AND parts[3] = 'presence' AND owner_id = in_resource_id
    WHEN 'OUTBOX_RECORDED' THEN in_resource_type = 'outbox'
      AND parts[1] = 'tenant' AND parts[3] = 'outbox' AND owner_id = in_organization_id
    ELSE false
  END;
EXCEPTION
  WHEN invalid_text_representation THEN
    RETURN false;
END
$function$;

CREATE FUNCTION nexora.outbox_safe_payload_v1_1_is_allowed(
  in_event_type nexora.outbox_event_type,
  in_resource_type text,
  in_payload jsonb
)
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
  safe_display jsonb;
  value_text text;
  expected_label text;
BEGIN
  IF jsonb_typeof(in_payload) <> 'object'
     OR NOT (in_payload ?& ARRAY[
       'resourceId', 'resourceType', 'organizationId', 'subjectId', 'actorId',
       'eventVersion', 'traceId', 'schemaVersion', 'safeDisplay'
     ]) THEN
    RETURN false;
  END IF;

  FOR key, value IN SELECT * FROM jsonb_each(in_payload) LOOP
    IF key NOT IN (
      'resourceId', 'resourceType', 'organizationId', 'subjectId', 'actorId',
      'eventVersion', 'jobState', 'progress', 'correlationId', 'traceId',
      'receiptId', 'schemaVersion', 'safeDisplay'
    ) THEN
      RETURN false;
    END IF;
  END LOOP;

  IF in_payload ->> 'resourceType' <> in_resource_type
     OR in_resource_type NOT IN ('page', 'job', 'notification', 'collaboration_session', 'outbox')
     OR in_payload ->> 'schemaVersion' <> '1.1.0'
     OR in_payload ->> 'resourceId' !~ '^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$'
     OR in_payload ->> 'organizationId' !~ '^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$'
     OR in_payload ->> 'subjectId' !~ '^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$'
     OR in_payload ->> 'actorId' !~ '^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$'
     OR in_payload ->> 'traceId' !~ '^[a-f0-9]{32}$'
     OR jsonb_typeof(in_payload -> 'eventVersion') <> 'number'
     OR (in_payload ->> 'eventVersion')::numeric < 1
     OR (in_payload ->> 'eventVersion')::numeric <> trunc((in_payload ->> 'eventVersion')::numeric) THEN
    RETURN false;
  END IF;

  IF (in_payload ? 'correlationId' AND in_payload ->> 'correlationId' !~ '^[a-f0-9]{32}$')
     OR (in_payload ? 'receiptId' AND in_payload ->> 'receiptId' !~ '^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$') THEN
    RETURN false;
  END IF;

  IF in_event_type = 'JOB_PROGRESS_CHANGED' THEN
    IF NOT (in_payload ?& ARRAY['jobState', 'progress'])
       OR in_payload ->> 'jobState' NOT IN ('QUEUED', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELED')
       OR jsonb_typeof(in_payload -> 'progress') <> 'number'
       OR (in_payload ->> 'progress')::numeric NOT BETWEEN 0 AND 100
       OR (in_payload ->> 'progress')::numeric <> trunc((in_payload ->> 'progress')::numeric) THEN
      RETURN false;
    END IF;
  ELSIF in_payload ? 'jobState' OR in_payload ? 'progress' THEN
    RETURN false;
  END IF;

  safe_display := in_payload -> 'safeDisplay';
  IF jsonb_typeof(safe_display) <> 'object'
     OR NOT (safe_display ?& ARRAY['label', 'status', 'variant'])
     OR EXISTS (
       SELECT 1
       FROM jsonb_object_keys(safe_display) AS display_key
       WHERE display_key NOT IN ('label', 'status', 'variant')
     )
     OR EXISTS (
       SELECT 1 FROM jsonb_each(safe_display) AS display_value
       WHERE jsonb_typeof(display_value.value) <> 'string'
     ) THEN
    RETURN false;
  END IF;

  expected_label := in_event_type::text;
  IF safe_display ->> 'label' <> expected_label THEN
    RETURN false;
  END IF;

  RETURN CASE in_event_type
    WHEN 'PUBLICATION_INVALIDATED' THEN (safe_display ->> 'status', safe_display ->> 'variant') IN (
      ('QUEUED', 'warning'), ('PUBLISHED', 'success'), ('ARCHIVED', 'neutral'), ('INVALIDATED', 'danger')
    )
    WHEN 'WORKFLOW_TRANSITIONED' THEN (safe_display ->> 'status', safe_display ->> 'variant') IN (
      ('PENDING', 'info'), ('IN_REVIEW', 'warning'), ('PUBLISHED', 'success'), ('ARCHIVED', 'neutral'), ('FAILED', 'danger')
    )
    WHEN 'JOB_PROGRESS_CHANGED' THEN (safe_display ->> 'status', safe_display ->> 'variant') IN (
      ('QUEUED', 'info'), ('RUNNING', 'warning'), ('COMPLETED', 'success'), ('FAILED', 'danger'), ('CANCELED', 'neutral')
    )
    WHEN 'NOTIFICATION_ENQUEUED' THEN (safe_display ->> 'status', safe_display ->> 'variant') IN (
      ('QUEUED', 'info'), ('DELIVERED', 'success'), ('FAILED', 'danger')
    )
    WHEN 'PRESENCE_CHANGED' THEN (safe_display ->> 'status', safe_display ->> 'variant') IN (
      ('ACTIVE', 'success'), ('INACTIVE', 'neutral')
    )
    WHEN 'OUTBOX_RECORDED' THEN (safe_display ->> 'status', safe_display ->> 'variant') IN (
      ('PENDING', 'info'), ('CLAIMED', 'warning'), ('PUBLISHED', 'success'), ('FAILED', 'danger'), ('DEAD_LETTER', 'danger')
    )
    ELSE false
  END;
EXCEPTION
  WHEN invalid_text_representation OR numeric_value_out_of_range THEN
    RETURN false;
END
$function$;

CREATE OR REPLACE FUNCTION nexora.guard_outbox_event_mutation()
RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = pg_catalog, nexora
AS $function$
BEGIN
  IF TG_OP = 'INSERT' THEN
    IF NEW.schema_version <> '1.1.0'
       OR NEW.state <> 'PENDING'
       OR NEW.attempt_count <> 0
       OR NEW.claim_owner IS NOT NULL
       OR NEW.claim_expires_at IS NOT NULL
       OR NEW.published_at IS NOT NULL
       OR NEW.failed_at IS NOT NULL
       OR NEW.dead_letter_at IS NOT NULL
       OR NEW.retain_until IS NOT NULL
       OR NEW.last_error_code IS NOT NULL
       OR NEW.idempotency_key_digest !~ '^sha256:[a-f0-9]{64}$'
       OR NEW.request_fingerprint_digest !~ '^sha256:[a-f0-9]{64}$'
       OR NEW.payload_digest !~ '^sha256:[a-f0-9]{64}$'
       OR NOT nexora.outbox_safe_payload_v1_1_is_allowed(NEW.event_type, NEW.resource_type, NEW.safe_payload)
       OR NOT nexora.outbox_event_route_v1_1_is_valid(NEW.event_type, NEW.resource_type, NEW.topic, NEW.organization_id, NEW.resource_id) THEN
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

  -- The migration itself can preserve, but never convert, active 1.0 rows.
  -- Runtime has no table DML and no function exposes this transition.
  IF OLD.schema_version <> '1.1.0'
     AND OLD.state IN ('PENDING', 'CLAIMED', 'FAILED')
     AND NEW.state = 'DEAD_LETTER'
     AND NEW.claim_owner IS NULL
     AND NEW.claim_expires_at IS NULL
     AND NEW.failed_at IS NOT NULL
     AND NEW.dead_letter_at IS NOT NULL
     AND NEW.retain_until > NEW.dead_letter_at
     AND NEW.last_error_code = 'LEGACY_SCHEMA_REJECTED' THEN
    RETURN NEW;
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
  ELSIF OLD.state = 'CLAIMED' AND NEW.state IN ('PUBLISHED', 'FAILED') THEN
    IF NEW.attempt_count <> OLD.attempt_count THEN
      RAISE EXCEPTION USING ERRCODE = '55000', MESSAGE = 'OUTBOX_TERMINAL_REJECTED';
    END IF;
  ELSIF OLD.state = 'FAILED' AND NEW.state = 'DEAD_LETTER' THEN
    IF OLD.attempt_count < 5
       OR NEW.attempt_count <> OLD.attempt_count
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

UPDATE nexora.outbox_events
SET state = 'DEAD_LETTER',
    claim_owner = NULL,
    claim_expires_at = NULL,
    failed_at = COALESCE(failed_at, transaction_timestamp()),
    dead_letter_at = transaction_timestamp(),
    retain_until = transaction_timestamp() + interval '90 days',
    last_error_code = 'LEGACY_SCHEMA_REJECTED',
    updated_at = transaction_timestamp()
WHERE schema_version <> '1.1.0'
  AND state IN ('PENDING', 'CLAIMED', 'FAILED');

CREATE OR REPLACE FUNCTION nexora.record_outbox_event(
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

  IF in_schema_version <> '1.1.0'
     OR in_event_version <= 0
     OR in_idempotency_key_digest !~ '^sha256:[a-f0-9]{64}$'
     OR in_request_fingerprint_digest !~ '^sha256:[a-f0-9]{64}$'
     OR in_payload_digest !~ '^sha256:[a-f0-9]{64}$'
     OR NOT nexora.outbox_event_route_v1_1_is_valid(in_event_type, in_resource_type, in_topic, in_organization_id, in_resource_id)
     OR NOT nexora.outbox_safe_payload_v1_1_is_allowed(in_event_type, in_resource_type, in_safe_payload) THEN
    RAISE EXCEPTION USING ERRCODE = '22023', MESSAGE = 'PAYLOAD_REJECTED';
  END IF;

  IF in_safe_payload ->> 'organizationId' <> in_organization_id::text
     OR in_safe_payload ->> 'subjectId' <> in_subject_id::text
     OR in_safe_payload ->> 'actorId' <> in_actor_id::text
     OR in_safe_payload ->> 'resourceId' <> in_resource_id::text
     OR in_safe_payload ->> 'resourceType' <> in_resource_type
     OR (in_safe_payload ->> 'eventVersion')::bigint <> in_event_version
     OR in_safe_payload ->> 'schemaVersion' <> in_schema_version THEN
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
  ) ON CONFLICT (organization_id, topic, event_type, idempotency_key_digest)
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
     OR existing.schema_version <> '1.1.0'
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

CREATE TABLE nexora.event_ledger_entries (
  event_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES nexora.organizations(id) ON DELETE RESTRICT,
  subject_id uuid NOT NULL,
  actor_id uuid NOT NULL,
  resource_type text NOT NULL CHECK (resource_type IN ('page', 'job', 'notification', 'collaboration_session', 'outbox')),
  resource_id uuid NOT NULL,
  event_type nexora.outbox_event_type NOT NULL,
  event_version bigint NOT NULL CHECK (event_version > 0),
  topic text NOT NULL,
  schema_version text NOT NULL CHECK (schema_version = '1.1.0'),
  idempotency_key_digest text NOT NULL CHECK (idempotency_key_digest ~ '^sha256:[a-f0-9]{64}$'),
  payload_digest text NOT NULL CHECK (payload_digest ~ '^sha256:[a-f0-9]{64}$'),
  safe_payload jsonb NOT NULL CHECK (jsonb_typeof(safe_payload) = 'object'),
  occurred_at timestamptz NOT NULL,
  persisted_at timestamptz NOT NULL DEFAULT transaction_timestamp(),
  CONSTRAINT event_ledger_entries_idempotency_uk UNIQUE (organization_id, topic, event_type, idempotency_key_digest)
);

ALTER TABLE nexora.event_ledger_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE nexora.event_ledger_entries FORCE ROW LEVEL SECURITY;

CREATE POLICY event_ledger_entries_migrator_control
ON nexora.event_ledger_entries
FOR ALL TO nexora_migrator
USING (true)
WITH CHECK (true);

CREATE INDEX event_ledger_entries_topic_lookup_idx
ON nexora.event_ledger_entries (organization_id, topic, occurred_at, event_id);

CREATE INDEX event_ledger_entries_resource_lookup_idx
ON nexora.event_ledger_entries (organization_id, resource_type, resource_id, occurred_at, event_id);

COMMENT ON TABLE nexora.event_ledger_entries IS
  'Private durable M3 consumer receipt ledger. Runtime has function-only access; M3-T05 recomputes canonical payload digests before invoking the record function.';

CREATE FUNCTION nexora.record_event_ledger_entry(
  in_event_id uuid,
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
  in_payload_digest text,
  in_safe_payload jsonb,
  in_occurred_at timestamptz
)
RETURNS TABLE (event_id uuid, duplicate boolean)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, nexora
AS $function$
DECLARE
  current_organization uuid;
  current_subject uuid;
  current_membership uuid;
  existing nexora.event_ledger_entries%ROWTYPE;
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

  IF in_schema_version <> '1.1.0'
     OR in_event_version <= 0
     OR in_idempotency_key_digest !~ '^sha256:[a-f0-9]{64}$'
     OR in_payload_digest !~ '^sha256:[a-f0-9]{64}$'
     OR NOT nexora.outbox_event_route_v1_1_is_valid(in_event_type, in_resource_type, in_topic, in_organization_id, in_resource_id)
     OR NOT nexora.outbox_safe_payload_v1_1_is_allowed(in_event_type, in_resource_type, in_safe_payload)
     OR in_safe_payload ->> 'organizationId' <> in_organization_id::text
     OR in_safe_payload ->> 'subjectId' <> in_subject_id::text
     OR in_safe_payload ->> 'actorId' <> in_actor_id::text
     OR in_safe_payload ->> 'resourceId' <> in_resource_id::text
     OR in_safe_payload ->> 'resourceType' <> in_resource_type
     OR (in_safe_payload ->> 'eventVersion')::bigint <> in_event_version
     OR in_safe_payload ->> 'schemaVersion' <> in_schema_version THEN
    RAISE EXCEPTION USING ERRCODE = '22023', MESSAGE = 'PAYLOAD_REJECTED';
  END IF;

  INSERT INTO nexora.event_ledger_entries (
    event_id, organization_id, subject_id, actor_id, resource_type, resource_id,
    event_type, event_version, topic, schema_version, idempotency_key_digest,
    payload_digest, safe_payload, occurred_at
  ) VALUES (
    in_event_id, in_organization_id, in_subject_id, in_actor_id,
    in_resource_type, in_resource_id, in_event_type, in_event_version,
    in_topic, in_schema_version, in_idempotency_key_digest, in_payload_digest,
    in_safe_payload, in_occurred_at
  ) ON CONFLICT DO NOTHING;

  IF FOUND THEN
    RETURN QUERY SELECT in_event_id, false;
    RETURN;
  END IF;

  SELECT * INTO existing
  FROM nexora.event_ledger_entries AS ledger
  WHERE ledger.event_id = in_event_id;

  IF FOUND THEN
    IF ROW(
      existing.organization_id, existing.subject_id, existing.actor_id,
      existing.resource_type, existing.resource_id, existing.event_type,
      existing.event_version, existing.topic, existing.schema_version,
      existing.idempotency_key_digest, existing.payload_digest,
      existing.safe_payload, existing.occurred_at
    ) IS DISTINCT FROM ROW(
      in_organization_id, in_subject_id, in_actor_id, in_resource_type,
      in_resource_id, in_event_type, in_event_version, in_topic,
      in_schema_version, in_idempotency_key_digest, in_payload_digest,
      in_safe_payload, in_occurred_at
    ) THEN
      RAISE EXCEPTION USING ERRCODE = '23505', MESSAGE = 'EVENT_ID_REUSED';
    END IF;
    RETURN QUERY SELECT existing.event_id, true;
    RETURN;
  END IF;

  SELECT * INTO existing
  FROM nexora.event_ledger_entries AS ledger
  WHERE ledger.organization_id = in_organization_id
    AND ledger.topic = in_topic
    AND ledger.event_type = in_event_type
    AND ledger.idempotency_key_digest = in_idempotency_key_digest;

  IF NOT FOUND
     OR existing.payload_digest IS DISTINCT FROM in_payload_digest
     OR existing.subject_id IS DISTINCT FROM in_subject_id
     OR existing.resource_id IS DISTINCT FROM in_resource_id
     OR existing.event_version IS DISTINCT FROM in_event_version THEN
    RAISE EXCEPTION USING ERRCODE = '23505', MESSAGE = 'IDEMPOTENCY_KEY_REUSED';
  END IF;

  RETURN QUERY SELECT existing.event_id, true;
END
$function$;

REVOKE ALL ON TABLE nexora.event_ledger_entries FROM PUBLIC;
REVOKE ALL ON FUNCTION nexora.outbox_event_route_v1_1_is_valid(nexora.outbox_event_type, text, text, uuid, uuid) FROM PUBLIC;
REVOKE ALL ON FUNCTION nexora.outbox_safe_payload_v1_1_is_allowed(nexora.outbox_event_type, text, jsonb) FROM PUBLIC;
REVOKE ALL ON FUNCTION nexora.record_event_ledger_entry(uuid, uuid, uuid, uuid, text, uuid, nexora.outbox_event_type, bigint, text, text, text, text, jsonb, timestamptz) FROM PUBLIC;
REVOKE ALL ON FUNCTION nexora.outbox_safe_payload_is_allowed(jsonb) FROM nexora_runtime;

GRANT EXECUTE ON FUNCTION nexora.outbox_event_route_v1_1_is_valid(nexora.outbox_event_type, text, text, uuid, uuid) TO nexora_migrator;
GRANT EXECUTE ON FUNCTION nexora.outbox_safe_payload_v1_1_is_allowed(nexora.outbox_event_type, text, jsonb) TO nexora_migrator;
GRANT EXECUTE ON FUNCTION nexora.outbox_safe_payload_v1_1_is_allowed(nexora.outbox_event_type, text, jsonb) TO nexora_runtime;
GRANT EXECUTE ON FUNCTION nexora.record_event_ledger_entry(uuid, uuid, uuid, uuid, text, uuid, nexora.outbox_event_type, bigint, text, text, text, text, jsonb, timestamptz) TO nexora_runtime;

DO $revoke_event_ledger_api_roles$
DECLARE
  api_role text;
BEGIN
  FOREACH api_role IN ARRAY ARRAY['anon', 'authenticated', 'service_role'] LOOP
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = api_role) THEN
      EXECUTE format('REVOKE ALL ON TABLE nexora.event_ledger_entries FROM %I', api_role);
      EXECUTE format('REVOKE ALL ON FUNCTION nexora.record_event_ledger_entry(uuid, uuid, uuid, uuid, text, uuid, nexora.outbox_event_type, bigint, text, text, text, text, jsonb, timestamptz) FROM %I', api_role);
    END IF;
  END LOOP;
END
$revoke_event_ledger_api_roles$;

RESET ROLE;
COMMIT;
