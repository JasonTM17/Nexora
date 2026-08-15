-- M4-DB01 fixture: deterministic knowledge/RAG policy proof on the disposable
-- local stand-in. Two tenants (the M2 fixture's Alpha University and Beta
-- Institute), both permission tiers, cross-tenant denial and deletion
-- eligibility. Seeds run as nexora_runtime with a resolved transaction-local
-- context, matching the M2 fixture pattern; proofs run the same way under
-- forced RLS. Synthetic values only.

-- Tenant A = Alpha University (10000000-...-0001). Owner subject is the M2
-- fixture ADMIN (knowledge.manage + read); member subject is its REVIEWER
-- (knowledge.read only).
-- Tenant B = Beta Institute (10000000-...-0002). Owner subject is its OWNER.

BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000001', true);

INSERT INTO nexora.knowledge_bases (id, organization_id, name, description, state, created_by_subject_id) VALUES
  ('11000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'Acme Knowledge', '', 'ACTIVE', '20000000-0000-4000-8000-000000000001');

INSERT INTO nexora.documents (id, knowledge_base_id, organization_id, original_name, stored_object_key, content_type, byte_size, sha256, state, uploaded_by_subject_id) VALUES
  ('30000000-0000-4000-8000-000000000001', '11000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'syllabus.txt', 'organizations/10000000-0000-4000-8000-000000000001/knowledge/11000000-0000-4000-8000-000000000001/documents/30000000-0000-4000-8000-000000000001', 'text/plain', 512, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'READY', '20000000-0000-4000-8000-000000000001');

INSERT INTO nexora.chunks (id, document_id, organization_id, knowledge_base_id, chunk_index, text, token_count, sha256, chunk_strategy_version, state) VALUES
  ('40000000-0000-4000-8000-000000000001', '30000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', '11000000-0000-4000-8000-000000000001', 0, 'Nexora University publishes immutable page versions through a review workflow.', 12, 'dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd', 'nexora-chunk-v1', 'ACTIVE');

INSERT INTO nexora.chat_sessions (id, organization_id, subject_id, title, state) VALUES
  ('50000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000001', 'Publishing help', 'ACTIVE');

INSERT INTO nexora.chat_messages (id, session_id, organization_id, subject_id, client_message_id, client_message_id_digest, role, state, content) VALUES
  ('61000000-0000-4000-8000-000000000001', '50000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000001', 'msg-1', 'sha256:1111111111111111111111111111111111111111111111111111111111111111', 'user', 'COMPLETED', 'How do I publish immutably?');

COMMIT;

BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000003', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000002', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000004', true);

INSERT INTO nexora.knowledge_bases (id, organization_id, name, description, state, created_by_subject_id) VALUES
  ('21000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000002', 'Beta Knowledge', '', 'ACTIVE', '20000000-0000-4000-8000-000000000003');

INSERT INTO nexora.documents (id, knowledge_base_id, organization_id, original_name, stored_object_key, content_type, byte_size, sha256, state, uploaded_by_subject_id) VALUES
  ('31000000-0000-4000-8000-000000000001', '21000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000002', 'roadmap.md', 'organizations/10000000-0000-4000-8000-000000000002/knowledge/21000000-0000-4000-8000-000000000001/documents/31000000-0000-4000-8000-000000000001', 'text/markdown', 256, 'cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc', 'READY', '20000000-0000-4000-8000-000000000003');

INSERT INTO nexora.chunks (id, document_id, organization_id, knowledge_base_id, chunk_index, text, token_count, sha256, chunk_strategy_version, state) VALUES
  ('41000000-0000-4000-8000-000000000001', '31000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000002', '21000000-0000-4000-8000-000000000001', 0, 'The internal roadmap names the next quarter''s private release targets.', 12, 'eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee', 'nexora-chunk-v1', 'ACTIVE');

INSERT INTO nexora.chat_sessions (id, organization_id, subject_id, title, state) VALUES
  ('51000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000002', '20000000-0000-4000-8000-000000000003', 'Roadmap questions', 'ACTIVE');

COMMIT;

-- Runtime checks: tenant A admin (manage+read) sees its rows; tenant A
-- reviewer (read only) can read but not insert; tenant B owner sees nothing
-- of tenant A and the cross-tenant chunk probe is empty.

\echo '--- M4 knowledge RLS checks ---'

BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000001', true);

-- Owner reads its own ACTIVE chunk (knowledge.read) and sees exactly one.
DO $$
DECLARE
  visible integer;
BEGIN
  SELECT count(*) INTO visible FROM nexora.chunks
  WHERE organization_id = '10000000-0000-4000-8000-000000000001' AND state = 'ACTIVE';
  IF visible <> 1 THEN
    RAISE EXCEPTION 'tenant A owner expected 1 visible chunk, found %', visible;
  END IF;
END $$;

-- Owner may insert a new document (knowledge.manage).
INSERT INTO nexora.documents (id, knowledge_base_id, organization_id, original_name, stored_object_key, content_type, byte_size, sha256, state, uploaded_by_subject_id) VALUES
  ('30000000-0000-4000-8000-000000000009', '11000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'extra.txt', 'k/extra', 'text/plain', 1, 'ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff', 'UPLOADED', '20000000-0000-4000-8000-000000000001');

COMMIT;

-- Tenant B owner under tenant B context: can read its own roadmap chunk but
-- must see nothing from tenant A even with a direct id probe.
BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000003', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000002', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000004', true);

DO $$
DECLARE
  visible integer;
BEGIN
  SELECT count(*) INTO visible FROM nexora.chunks
  WHERE id = '40000000-0000-4000-8000-000000000001';
  IF visible <> 0 THEN
    RAISE EXCEPTION 'cross-tenant chunk probe leaked % rows', visible;
  END IF;
END $$;

DO $$
DECLARE
  own integer;
BEGIN
  SELECT count(*) INTO own FROM nexora.chunks
  WHERE organization_id = '10000000-0000-4000-8000-000000000002' AND state = 'ACTIVE';
  IF own <> 1 THEN
    RAISE EXCEPTION 'tenant B owner expected 1 own chunk, found %', own;
  END IF;
END $$;

COMMIT;

-- Read-only reviewer of tenant A: reads pass (knowledge.read), inserts fail
-- (no knowledge.manage).
BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000002', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000003', true);

DO $$
DECLARE
  visible integer;
BEGIN
  SELECT count(*) INTO visible FROM nexora.chunks
  WHERE organization_id = '10000000-0000-4000-8000-000000000001' AND state = 'ACTIVE';
  IF visible <> 1 THEN
    RAISE EXCEPTION 'tenant A reviewer expected 1 visible chunk, found %', visible;
  END IF;
END $$;

DO $$
DECLARE
  inserted boolean := false;
BEGIN
  BEGIN
    INSERT INTO nexora.documents (id, knowledge_base_id, organization_id, original_name, stored_object_key, content_type, byte_size, sha256, state, uploaded_by_subject_id) VALUES
      ('30000000-0000-4000-8000-000000000010', '11000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'denied.txt', 'k/denied', 'text/plain', 1, '0000000000000000000000000000000000000000000000000000000000000000', 'UPLOADED', '20000000-0000-4000-8000-000000000002');
    inserted := true;
  EXCEPTION WHEN insufficient_privilege THEN
    inserted := false;
  END;
  IF inserted THEN
    RAISE EXCEPTION 'read-only reviewer inserted a document without knowledge.manage';
  END IF;
END $$;

COMMIT;

-- Deletion eligibility and resurrection denial: a DELETED document is
-- unreadable to knowledge.read-only subjects through forced RLS (managers
-- keep the trash view so soft-delete UPDATEs stay visible), and a terminal
-- guard rejects flipping it back to an eligible state.

BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000001', true);

UPDATE nexora.documents SET state = 'DELETED'
WHERE id = '30000000-0000-4000-8000-000000000001';

COMMIT;

-- Read-only reviewer: the DELETED document and its chunk must be invisible.
BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000002', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000003', true);

DO $$
DECLARE
  visible integer;
BEGIN
  SELECT count(*) INTO visible FROM nexora.documents
  WHERE id = '30000000-0000-4000-8000-000000000001';
  IF visible <> 0 THEN
    RAISE EXCEPTION 'DELETED document stayed visible to a read-only subject';
  END IF;
  SELECT count(*) INTO visible FROM nexora.chunks
  WHERE document_id = '30000000-0000-4000-8000-000000000001';
  IF visible <> 0 THEN
    RAISE EXCEPTION 'chunk of DELETED document stayed visible to a read-only subject';
  END IF;
END $$;

COMMIT;

-- Manager (trash view): the DELETED row stays visible and the resurrection
-- guard rejects flipping it back to an eligible state.
BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000001', true);

DO $$
DECLARE
  visible integer;
BEGIN
  SELECT count(*) INTO visible FROM nexora.documents
  WHERE id = '30000000-0000-4000-8000-000000000001';
  IF visible <> 1 THEN
    RAISE EXCEPTION 'manager trash view lost the DELETED document';
  END IF;
END $$;

DO $$
DECLARE
  resurrected boolean := false;
BEGIN
  BEGIN
    UPDATE nexora.documents SET state = 'READY'
    WHERE id = '30000000-0000-4000-8000-000000000001';
    resurrected := true;
  EXCEPTION WHEN integrity_constraint_violation THEN
    resurrected := false;
  END;
  IF resurrected THEN
    RAISE EXCEPTION 'DELETED document was resurrected to READY';
  END IF;
END $$;

COMMIT;

-- Active-plane dedup: re-upload of identical bytes+name after deletion is
-- allowed because the partial index only covers non-DELETED rows.

BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000001', true);

INSERT INTO nexora.documents (id, knowledge_base_id, organization_id, original_name, stored_object_key, content_type, byte_size, sha256, state, uploaded_by_subject_id) VALUES
  ('30000000-0000-4000-8000-000000000011', '11000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'syllabus.txt', 'k/reupload', 'text/plain', 512, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'UPLOADED', '20000000-0000-4000-8000-000000000001');

COMMIT;

-- Chat subject scoping: a second tenant-A subject with knowledge.read must
-- not write into another subject's session.

BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000002', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000003', true);

DO $$
DECLARE
  inserted boolean := false;
BEGIN
  BEGIN
    INSERT INTO nexora.chat_messages (id, session_id, organization_id, subject_id, client_message_id, client_message_id_digest, role, state, content) VALUES
      ('61000000-0000-4000-8000-000000000002', '50000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000002', 'probe', 'sha256:2222222222222222222222222222222222222222222222222222222222222222', 'user', 'DRAFT', 'probe into another subject session');
    inserted := true;
  EXCEPTION WHEN insufficient_privilege OR foreign_key_violation THEN
    inserted := false;
  END;
  IF inserted THEN
    RAISE EXCEPTION 'subject wrote a message into another subject session';
  END IF;
END $$;

COMMIT;

-- Structural assertions: forced RLS everywhere, no SECURITY DEFINER, no
-- API-role grants on the M4 tables.

BEGIN;
SET LOCAL ROLE nexora_migrator;

DO $$
DECLARE
  missing text;
BEGIN
  FOREACH missing IN ARRAY ARRAY[
    'knowledge_bases', 'documents', 'document_jobs', 'chunks',
    'chat_sessions', 'chat_messages', 'retrieval_runs', 'chunk_vectors'
  ] LOOP
    IF NOT EXISTS (
      SELECT 1 FROM pg_class
      WHERE relname = missing
        AND relrowsecurity AND relforcerowsecurity
    ) THEN
      RAISE EXCEPTION 'table % lacks enabled forced RLS', missing;
    END IF;
  END LOOP;

  IF EXISTS (
    SELECT 1 FROM pg_proc
    WHERE pronamespace = 'nexora'::regnamespace
      AND prosecdef
      AND proname IN ('guard_knowledge_terminal_transition', 'guard_document_job_parent_state',
                      'knowledge_current_tenant_has_permission')
  ) THEN
    RAISE EXCEPTION 'M4 guard helper must not be SECURITY DEFINER';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM information_schema.role_table_grants
    WHERE grantee IN ('anon', 'authenticated', 'service_role')
      AND table_schema = 'nexora'
      AND table_name IN ('knowledge_bases', 'documents', 'document_jobs', 'chunks',
                         'chat_sessions', 'chat_messages', 'retrieval_runs')
  ) THEN
    RAISE EXCEPTION 'Data API role holds a grant on an M4 table';
  END IF;
END $$;

COMMIT;

-- Document jobs: the terminal shape checks are enforced and the parent guard
-- blocks a job mutation after its document is DELETED.

BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000001', true);

INSERT INTO nexora.document_jobs (id, document_id, organization_id, state, max_attempts) VALUES
  ('80000000-0000-4000-8000-000000000001', '30000000-0000-4000-8000-000000000011', '10000000-0000-4000-8000-000000000001', 'QUEUED', 5);

UPDATE nexora.document_jobs SET state = 'RUNNING', heartbeat_at = transaction_timestamp()
WHERE id = '80000000-0000-4000-8000-000000000001';

DO $$
DECLARE
  bad_shape boolean := false;
BEGIN
  BEGIN
    UPDATE nexora.document_jobs SET state = 'SUCCEEDED', last_error_code = 'INGESTION_FAILED'
    WHERE id = '80000000-0000-4000-8000-000000000001';
    bad_shape := true;
  EXCEPTION WHEN check_violation THEN
    bad_shape := false;
  END;
  IF bad_shape THEN
    RAISE EXCEPTION 'SUCCEEDED job accepted a last_error_code';
  END IF;
END $$;

UPDATE nexora.document_jobs SET state = 'FAILED', last_error_code = 'INGESTION_FAILED', heartbeat_at = NULL
WHERE id = '80000000-0000-4000-8000-000000000001';

DO $$
DECLARE
  failed_count integer;
BEGIN
  SELECT count(*) INTO failed_count FROM nexora.document_jobs
  WHERE id = '80000000-0000-4000-8000-000000000001'
    AND state = 'FAILED' AND last_error_code = 'INGESTION_FAILED';
  IF failed_count IS NULL OR failed_count != 1 THEN
    RAISE EXCEPTION 'FAILED job did not persist its error code';
  END IF;
END $$;

COMMIT;

-- Vector plane: a deterministic 1024-dim row is insertable, the installed
-- pgvector version is observed and recorded, and the runtime can see the row
-- through forced RLS.

BEGIN;
SET LOCAL ROLE nexora_migrator;

DO $$
DECLARE
  installed text;
BEGIN
  SELECT default_version INTO installed FROM pg_available_extensions
  WHERE name = 'vector';
  IF installed IS NULL OR installed = '' THEN
    RAISE EXCEPTION 'vector extension is not available in this environment';
  END IF;
  RAISE NOTICE 'observed pgvector version: %', installed;
END $$;

COMMIT;

BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000001', true);

INSERT INTO nexora.chunks (id, document_id, organization_id, knowledge_base_id, chunk_index, text, token_count, sha256, chunk_strategy_version, state) VALUES
  ('40000000-0000-4000-8000-000000000002', '30000000-0000-4000-8000-000000000011', '10000000-0000-4000-8000-000000000001', '11000000-0000-4000-8000-000000000001', 0, 'A re-uploaded syllabus chunk for vector proof.', 8, '9999999999999999999999999999999999999999999999999999999999999999', 'nexora-chunk-v1', 'ACTIVE');

INSERT INTO rag.chunk_vectors (id, chunk_id, document_id, organization_id, knowledge_base_id, model_id, model_revision, dimensions, embedding, sha256, state) VALUES
  ('70000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000002', '30000000-0000-4000-8000-000000000011', '10000000-0000-4000-8000-000000000001', '11000000-0000-4000-8000-000000000001', 'qwen3-embedding-0.6b', 'tbd-fixture-revision', 1024, ARRAY(SELECT 0.001::real FROM generate_series(1, 1024))::vector, '8888888888888888888888888888888888888888888888888888888888888888', 'ACTIVE');

DO $$
DECLARE
  vec_count integer;
BEGIN
  SELECT count(*) INTO vec_count FROM rag.chunk_vectors
  WHERE organization_id = '10000000-0000-4000-8000-000000000001' AND state = 'ACTIVE';
  IF vec_count IS NULL OR vec_count != 1 THEN
    RAISE EXCEPTION 'expected 1 ACTIVE vector row, found %', vec_count;
  END IF;
END $$;

COMMIT;

-- Chunk and vector terminal guards: SUPERSEDED rows can never become ACTIVE
-- again, and a MERGE cannot bypass the row trigger.

BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000001', true);

UPDATE nexora.chunks SET state = 'SUPERSEDED'
WHERE id = '40000000-0000-4000-8000-000000000002';

UPDATE rag.chunk_vectors SET state = 'SUPERSEDED'
WHERE chunk_id = '40000000-0000-4000-8000-000000000002';

DO $$
DECLARE
  resurrected boolean := false;
BEGIN
  BEGIN
    UPDATE nexora.chunks SET state = 'ACTIVE'
    WHERE id = '40000000-0000-4000-8000-000000000002';
    resurrected := true;
  EXCEPTION WHEN integrity_constraint_violation THEN
    resurrected := false;
  END;
  IF resurrected THEN
    RAISE EXCEPTION 'SUPERSEDED chunk was resurrected to ACTIVE';
  END IF;
  BEGIN
    MERGE INTO nexora.chunks AS target
    USING (VALUES ('40000000-0000-4000-8000-000000000002'::uuid)) AS source (id)
    ON target.id = source.id
    WHEN MATCHED THEN UPDATE SET state = 'ACTIVE';
    resurrected := true;
  EXCEPTION WHEN integrity_constraint_violation THEN
    resurrected := false;
  END;
  IF resurrected THEN
    RAISE EXCEPTION 'MERGE bypassed the chunk terminal guard';
  END IF;
  BEGIN
    UPDATE rag.chunk_vectors SET state = 'ACTIVE'
    WHERE chunk_id = '40000000-0000-4000-8000-000000000002';
    resurrected := true;
  EXCEPTION WHEN integrity_constraint_violation THEN
    resurrected := false;
  END;
  IF resurrected THEN
    RAISE EXCEPTION 'SUPERSEDED vector was resurrected to ACTIVE';
  END IF;
END $$;

COMMIT;
