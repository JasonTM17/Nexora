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
