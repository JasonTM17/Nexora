\set ON_ERROR_STOP on

-- M3-DB01 local fixture. The verifier creates a disposable stand-in for only
-- auth.uid(), realtime.topic(), and realtime.messages before V014 is applied.
-- This is policy-conformance evidence, not a hosted Supabase claim.

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_class relation
    JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
    WHERE namespace.nspname = 'nexora'
      AND relation.relname = 'outbox_events'
      AND relation.relrowsecurity
      AND relation.relforcerowsecurity
  ) THEN
    RAISE EXCEPTION 'outbox_events must enable and force RLS';
  END IF;

  IF has_table_privilege('nexora_runtime', 'nexora.outbox_events', 'SELECT')
     OR has_table_privilege('nexora_runtime', 'nexora.outbox_events', 'INSERT')
     OR has_table_privilege('nexora_runtime', 'nexora.outbox_events', 'UPDATE')
     OR has_table_privilege('nexora_runtime', 'nexora.outbox_events', 'DELETE') THEN
    RAISE EXCEPTION 'runtime must have function-only outbox table access';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_policy
    WHERE polrelid = 'realtime.messages'::regclass
      AND polname = 'realtime_messages_select_private_channels'
  ) OR NOT EXISTS (
    SELECT 1 FROM pg_policy
    WHERE polrelid = 'realtime.messages'::regclass
      AND polname = 'realtime_messages_insert_private_channels'
  ) THEN
    RAISE EXCEPTION 'provider-style private Realtime policies are missing';
  END IF;

  IF has_function_privilege('authenticated', 'nexora.realtime_private_channel_authorized(text, uuid)', 'EXECUTE') THEN
    RAISE EXCEPTION 'authenticated can probe arbitrary private Realtime subjects';
  END IF;
  IF NOT has_function_privilege('authenticated', 'nexora.realtime_current_channel_authorized(text)', 'EXECUTE') THEN
    RAISE EXCEPTION 'authenticated cannot execute the current-topic Realtime policy helper';
  END IF;

  IF EXISTS (
    SELECT 1 FROM pg_proc procedure
    JOIN pg_namespace namespace ON namespace.oid = procedure.pronamespace
    WHERE namespace.nspname = 'nexora'
      AND procedure.proname IN (
        'record_outbox_event', 'claim_outbox_events',
        'publish_claimed_outbox_event', 'fail_claimed_outbox_event',
        'dead_letter_failed_outbox_event',
        'realtime_private_channel_authorized', 'realtime_current_channel_authorized'
      )
      AND NOT procedure.prosecdef
  ) THEN
    RAISE EXCEPTION 'outbox authority functions must use the reviewed definer boundary';
  END IF;
END
$$;

BEGIN;
SET LOCAL ROLE nexora_runtime;

DO $$
BEGIN
  BEGIN
    PERFORM count(*) FROM nexora.outbox_events;
    RAISE EXCEPTION 'runtime directly read the private outbox table';
  EXCEPTION WHEN insufficient_privilege THEN
    NULL;
  END;
END
$$;

SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000007', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000007', true);

DO $$
DECLARE
  publication_id uuid;
  repeated_id uuid;
  job_id uuid;
  claimed nexora.outbox_events%ROWTYPE;
  published nexora.outbox_events%ROWTYPE;
  failed nexora.outbox_events%ROWTYPE;
  dead_letter nexora.outbox_events%ROWTYPE;
  attempt integer;
  retry_floor interval;
  retry_ceiling interval;
BEGIN
  IF NOT nexora.outbox_safe_payload_is_allowed(
    '{
      "resourceId":"30000000-0000-4000-8000-000000000001",
      "resourceType":"page",
      "organizationId":"10000000-0000-4000-8000-000000000001",
      "subjectId":"20000000-0000-4000-8000-000000000007",
      "actorId":"20000000-0000-4000-8000-000000000007",
      "eventVersion":1,
      "correlationId":"corr-alpha-001",
      "traceId":"trace-outbox-alpha-001",
      "receiptId":"receipt-alpha-001",
      "schemaVersion":"1.0.0",
      "safeDisplay":{
        "label":"Alpha publication invalidated",
        "status":"queued",
        "hint":"Awaiting durable publication",
        "variant":"warning"
      }
    }'::jsonb
  ) THEN
    RAISE EXCEPTION 'valid safe payload was rejected';
  END IF;

  IF nexora.outbox_safe_payload_is_allowed('{"body":"secret content"}'::jsonb)
     OR nexora.outbox_safe_payload_is_allowed('{"safeDisplay":{"label":"Bearer leaked-token","status":"queued"}}'::jsonb)
     OR nexora.outbox_safe_payload_is_allowed('{"safeDisplay":{}}'::jsonb)
     OR nexora.outbox_safe_payload_is_allowed('{"safeDisplay":{"label":"Alpha","status":{"nested":"queued"}}}'::jsonb)
     OR nexora.outbox_safe_payload_is_allowed('{"safeDisplay":{"label":"Alpha","status":"queued","variant":"purple"}}'::jsonb)
     OR nexora.outbox_safe_payload_is_allowed('{"traceId":"Bearer token should not survive"}'::jsonb) THEN
    RAISE EXCEPTION 'unsafe payload or sensitive display value passed';
  END IF;

  publication_id := nexora.record_outbox_event(
    '50000000-0000-4000-8000-000000000001',
    '10000000-0000-4000-8000-000000000001',
    '20000000-0000-4000-8000-000000000007',
    '20000000-0000-4000-8000-000000000007',
    'page',
    '30000000-0000-4000-8000-000000000001',
    'PUBLICATION_INVALIDATED',
    1,
    'tenant:10000000-0000-4000-8000-000000000001:publication',
    '1.0.0',
    'sha256:event-contract-alpha-publication',
    'sha256:event-contract-alpha-publication-fingerprint',
    'sha256:1111111111111111111111111111111111111111111111111111111111111111',
    '{
      "resourceId":"30000000-0000-4000-8000-000000000001",
      "resourceType":"page",
      "organizationId":"10000000-0000-4000-8000-000000000001",
      "subjectId":"20000000-0000-4000-8000-000000000007",
      "actorId":"20000000-0000-4000-8000-000000000007",
      "eventVersion":1,
      "correlationId":"corr-alpha-001",
      "traceId":"trace-outbox-alpha-001",
      "receiptId":"receipt-alpha-001",
      "schemaVersion":"1.0.0",
      "safeDisplay":{
        "label":"Alpha publication invalidated",
        "status":"queued",
        "hint":"Awaiting durable publication",
        "variant":"warning"
      }
    }'::jsonb,
    '2026-08-10T00:10:00Z'
  );

  repeated_id := nexora.record_outbox_event(
    '50000000-0000-4000-8000-000000000099',
    '10000000-0000-4000-8000-000000000001',
    '20000000-0000-4000-8000-000000000007',
    '20000000-0000-4000-8000-000000000007',
    'page',
    '30000000-0000-4000-8000-000000000001',
    'PUBLICATION_INVALIDATED',
    1,
    'tenant:10000000-0000-4000-8000-000000000001:publication',
    '1.0.0',
    'sha256:event-contract-alpha-publication',
    'sha256:event-contract-alpha-publication-fingerprint',
    'sha256:1111111111111111111111111111111111111111111111111111111111111111',
    '{
      "resourceId":"30000000-0000-4000-8000-000000000001",
      "resourceType":"page",
      "organizationId":"10000000-0000-4000-8000-000000000001",
      "subjectId":"20000000-0000-4000-8000-000000000007",
      "actorId":"20000000-0000-4000-8000-000000000007",
      "eventVersion":1,
      "correlationId":"corr-alpha-001",
      "traceId":"trace-outbox-alpha-001",
      "receiptId":"receipt-alpha-001",
      "schemaVersion":"1.0.0",
      "safeDisplay":{
        "label":"Alpha publication invalidated",
        "status":"queued",
        "hint":"Awaiting durable publication",
        "variant":"warning"
      }
    }'::jsonb,
    '2026-08-10T00:10:00Z'
  );

  IF publication_id IS DISTINCT FROM repeated_id THEN
    RAISE EXCEPTION 'same request did not reuse the original outbox receipt';
  END IF;

  BEGIN
    PERFORM nexora.record_outbox_event(
      '50000000-0000-4000-8000-000000000098',
      '10000000-0000-4000-8000-000000000001',
      '20000000-0000-4000-8000-000000000007',
      '20000000-0000-4000-8000-000000000007',
      'page',
      '30000000-0000-4000-8000-000000000001',
      'PUBLICATION_INVALIDATED',
      1,
      'tenant:10000000-0000-4000-8000-000000000001:publication',
      '1.0.0',
      'sha256:event-contract-alpha-publication',
      'sha256:different-request-fingerprint',
      'sha256:1111111111111111111111111111111111111111111111111111111111111111',
      '{}'::jsonb,
      '2026-08-10T00:10:00Z'
    );
    RAISE EXCEPTION 'changed request reused an idempotency key';
  EXCEPTION WHEN unique_violation THEN
    NULL;
  END;

  BEGIN
    PERFORM nexora.record_outbox_event(
      '50000000-0000-4000-8000-000000000096',
      '10000000-0000-4000-8000-000000000001',
      '20000000-0000-4000-8000-000000000007',
      '20000000-0000-4000-8000-000000000007',
      'page',
      '30000000-0000-4000-8000-000000000001',
      'PUBLICATION_INVALIDATED',
      1,
      'tenant:10000000-0000-4000-8000-000000000001:presence',
      '1.0.0',
      'sha256:event-contract-alpha-misrouted',
      'sha256:event-contract-alpha-misrouted-fingerprint',
      'sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
      '{}'::jsonb,
      '2026-08-10T00:10:00Z'
    );
    RAISE EXCEPTION 'event type accepted a mismatched topic purpose';
  EXCEPTION WHEN invalid_parameter_value THEN
    NULL;
  END;

  BEGIN
    PERFORM nexora.record_outbox_event(
      '50000000-0000-4000-8000-000000000097',
      '10000000-0000-4000-8000-000000000002',
      '20000000-0000-4000-8000-000000000007',
      '20000000-0000-4000-8000-000000000007',
      'page',
      '30000000-0000-4000-8000-000000000001',
      'PUBLICATION_INVALIDATED', 1,
      'tenant:10000000-0000-4000-8000-000000000002:publication',
      '1.0.0', 'sha256:cross-tenant-key-0001',
      'sha256:cross-tenant-fingerprint-0001',
      'sha256:cross-tenant-payload-0001', '{}'::jsonb
    );
    RAISE EXCEPTION 'cross-tenant outbox record unexpectedly succeeded';
  EXCEPTION WHEN insufficient_privilege THEN
    NULL;
  END;

  job_id := nexora.record_outbox_event(
    '50000000-0000-4000-8000-000000000002',
    '10000000-0000-4000-8000-000000000001',
    '20000000-0000-4000-8000-000000000007',
    '20000000-0000-4000-8000-000000000007',
    'job',
    '50000000-0000-4000-8000-000000000010',
    'JOB_PROGRESS_CHANGED',
    1,
    'resource:50000000-0000-4000-8000-000000000010:job-progress',
    '1.0.0',
    'sha256:event-contract-alpha-job-0001',
    'sha256:event-contract-alpha-job-fingerprint-0001',
    'sha256:2222222222222222222222222222222222222222222222222222222222222222',
    '{
      "resourceId":"50000000-0000-4000-8000-000000000010",
      "resourceType":"job",
      "organizationId":"10000000-0000-4000-8000-000000000001",
      "subjectId":"20000000-0000-4000-8000-000000000007",
      "actorId":"20000000-0000-4000-8000-000000000007",
      "eventVersion":1,
      "jobState":"RUNNING",
      "progress":45,
      "schemaVersion":"1.0.0",
      "safeDisplay":{
        "label":"Alpha job progress",
        "status":"running",
        "state":"RUNNING",
        "progressText":"45%"
      }
    }'::jsonb
  );

  IF NOT nexora.realtime_private_channel_authorized(
    'tenant:10000000-0000-4000-8000-000000000001:publication',
    '20000000-0000-4000-8000-000000000007'
  ) OR NOT nexora.realtime_private_channel_authorized(
    'resource:50000000-0000-4000-8000-000000000010:job-progress',
    '20000000-0000-4000-8000-000000000007'
  ) OR nexora.realtime_private_channel_authorized(
    'tenant:10000000-0000-4000-8000-000000000002:publication',
    '20000000-0000-4000-8000-000000000007'
  ) THEN
    RAISE EXCEPTION 'private topic ownership helper produced an unsafe result';
  END IF;

  SELECT * INTO claimed
  FROM nexora.claim_outbox_events('publisher-alpha', interval '15 minutes', 1)
  LIMIT 1;
  IF claimed.id IS DISTINCT FROM publication_id OR claimed.attempt_count <> 1 THEN
    RAISE EXCEPTION 'first bounded claim was not deterministic';
  END IF;

  BEGIN
    PERFORM nexora.publish_claimed_outbox_event(claimed.id, 'publisher-other');
    RAISE EXCEPTION 'wrong publisher completed another owner lease';
  EXCEPTION WHEN object_not_in_prerequisite_state THEN
    NULL;
  END;

  published := nexora.publish_claimed_outbox_event(claimed.id, 'publisher-alpha');
  IF published.state <> 'PUBLISHED'
     OR published.retain_until <= published.published_at
     OR published.claim_owner IS NOT NULL THEN
    RAISE EXCEPTION 'published state or retention evidence is invalid';
  END IF;

  SELECT * INTO claimed
  FROM nexora.claim_outbox_events('publisher-job', interval '15 minutes', 1)
  LIMIT 1;
  IF claimed.id IS DISTINCT FROM job_id THEN
    RAISE EXCEPTION 'job event was not the next claimable row';
  END IF;

  failed := nexora.fail_claimed_outbox_event(
    claimed.id, 'publisher-job', 'BROKER_UNAVAILABLE'
  );
  IF failed.state <> 'FAILED'
     OR failed.last_error_code <> 'BROKER_UNAVAILABLE'
     OR failed.available_at <= clock_timestamp() + interval '500 milliseconds'
     OR failed.available_at > clock_timestamp() + interval '1500 milliseconds' THEN
    RAISE EXCEPTION 'retryable failure was not operator-visible';
  END IF;

  BEGIN
    PERFORM nexora.dead_letter_failed_outbox_event(failed.id, 'EARLY_DEAD_LETTER');
    RAISE EXCEPTION 'pre-max-attempt FAILED row reached DEAD_LETTER';
  EXCEPTION WHEN object_not_in_prerequisite_state THEN
    NULL;
  END;

  FOR attempt IN 2..5 LOOP
    PERFORM pg_sleep(CASE attempt
      WHEN 2 THEN 1.5
      WHEN 3 THEN 2.5
      WHEN 4 THEN 4.5
      ELSE 8.5
    END);

    SELECT * INTO claimed
    FROM nexora.claim_outbox_events('publisher-job-' || attempt::text, interval '15 minutes', 1)
    LIMIT 1;
    IF claimed.id IS DISTINCT FROM job_id OR claimed.attempt_count <> attempt THEN
      RAISE EXCEPTION 'retry attempt % did not re-enter bounded delivery', attempt;
    END IF;

    failed := nexora.fail_claimed_outbox_event(
      claimed.id, 'publisher-job-' || attempt::text, 'BROKER_UNAVAILABLE'
    );
    IF attempt < 5 THEN
      retry_floor := CASE attempt
        WHEN 2 THEN interval '1500 milliseconds'
        WHEN 3 THEN interval '3500 milliseconds'
        ELSE interval '7500 milliseconds'
      END;
      retry_ceiling := CASE attempt
        WHEN 2 THEN interval '2750 milliseconds'
        WHEN 3 THEN interval '4750 milliseconds'
        ELSE interval '8750 milliseconds'
      END;
    END IF;
    IF failed.state <> 'FAILED'
       OR failed.last_error_code <> 'BROKER_UNAVAILABLE'
       OR failed.attempt_count <> attempt
       OR (attempt < 5 AND (
         failed.available_at <= clock_timestamp() + retry_floor
         OR failed.available_at > clock_timestamp() + retry_ceiling
       )) THEN
      RAISE EXCEPTION 'attempt % did not retain FAILED evidence', attempt;
    END IF;
  END LOOP;

  dead_letter := nexora.dead_letter_failed_outbox_event(job_id, 'MAX_ATTEMPTS_EXHAUSTED');
  IF dead_letter.state <> 'DEAD_LETTER'
     OR dead_letter.attempt_count <> 5
     OR dead_letter.dead_letter_at IS NULL
     OR dead_letter.last_error_code <> 'MAX_ATTEMPTS_EXHAUSTED'
     OR dead_letter.retain_until <= dead_letter.dead_letter_at THEN
    RAISE EXCEPTION 'runtime FAILED to DEAD_LETTER evidence was not retained';
  END IF;
END
$$;
COMMIT;

BEGIN;
SET LOCAL ROLE nexora_migrator;
DO $$
BEGIN
  IF (SELECT count(*) FROM nexora.outbox_events) <> 2
     OR (SELECT count(*) FROM nexora.outbox_events WHERE state = 'PUBLISHED') <> 1
     OR (SELECT count(*) FROM nexora.outbox_events WHERE state = 'DEAD_LETTER') <> 1 THEN
    RAISE EXCEPTION 'final outbox projection does not retain both terminal outcomes';
  END IF;
END
$$;
COMMIT;

-- Exercise the actual provider-style policy expression against the disposable
-- stand-in. auth.uid() and realtime.topic() are trusted server-side helpers in
-- the hosted product; the local stand-in reads test-only transaction settings.
BEGIN;
SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '20000000-0000-4000-8000-000000000007', true);
SELECT set_config('realtime.topic', 'tenant:10000000-0000-4000-8000-000000000001:publication', true);

INSERT INTO realtime.messages (id, topic, payload)
VALUES (
  '60000000-0000-4000-8000-000000000001',
  'tenant:10000000-0000-4000-8000-000000000001:publication',
  '{"eventId":"50000000-0000-4000-8000-000000000001"}'::jsonb
);

DO $$
BEGIN
  IF (SELECT count(*) FROM realtime.messages) <> 1 THEN
    RAISE EXCEPTION 'authorized private Realtime row was not visible';
  END IF;

  BEGIN
    INSERT INTO realtime.messages (id, topic, payload)
    VALUES (
      '60000000-0000-4000-8000-000000000002',
      'tenant:10000000-0000-4000-8000-000000000001:workflow',
      '{}'::jsonb
    );
    RAISE EXCEPTION 'private Realtime insert ignored the current topic';
  EXCEPTION WHEN insufficient_privilege THEN
    NULL;
  END;

  PERFORM set_config('realtime.topic', 'tenant:10000000-0000-4000-8000-000000000001:workflow', true);
  INSERT INTO realtime.messages (id, topic, payload)
  VALUES (
    '60000000-0000-4000-8000-000000000003',
    'tenant:10000000-0000-4000-8000-000000000001:workflow',
    '{}'::jsonb
  );

  PERFORM set_config('realtime.topic', 'tenant:10000000-0000-4000-8000-000000000001:publication', true);
  IF (SELECT count(*) FROM realtime.messages) <> 1 THEN
    RAISE EXCEPTION 'authorized topic leaked a different private Realtime row';
  END IF;

  PERFORM set_config('realtime.topic', 'tenant:10000000-0000-4000-8000-000000000002:publication', true);
  IF (SELECT count(*) FROM realtime.messages) <> 0 THEN
    RAISE EXCEPTION 'guessed cross-tenant topic exposed a private Realtime row';
  END IF;

  BEGIN
    INSERT INTO realtime.messages (id, topic, payload)
    VALUES (
      '60000000-0000-4000-8000-000000000004',
      'tenant:10000000-0000-4000-8000-000000000002:publication',
      '{}'::jsonb
    );
    RAISE EXCEPTION 'cross-tenant private Realtime insert unexpectedly succeeded';
  EXCEPTION WHEN insufficient_privilege THEN
    NULL;
  END;
END
$$;
COMMIT;

SELECT 'M3-DB01 outbox and private Realtime verification passed' AS result,
       current_setting('server_version') AS postgresql_version;
