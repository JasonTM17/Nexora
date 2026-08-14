-- M4-DB01 hardening and vector plane: terminal-transition guards that make a
-- DELETED row non-resurrectable, version triggers, enum hygiene, the
-- application-owned vector table and pgvector compatibility evidence. No
-- managed-schema object and no explicit extension version clause is used.

BEGIN;
SET LOCAL ROLE nexora_migrator;
SET LOCAL search_path = pg_catalog, nexora, public;

-- Reject resurrection: once DELETED, a document, knowledge base, chat session
-- or chat message may never return to a retrieval-eligible state. FAILED
-- documents may only become QUEUED for a bounded re-attempt.
CREATE FUNCTION nexora.guard_knowledge_terminal_transition()
RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = pg_catalog, nexora
AS $function$
BEGIN
  IF OLD.state = 'DELETED' AND NEW.state <> 'DELETED' THEN
    RAISE EXCEPTION 'a DELETED % row cannot be resurrected', TG_TABLE_NAME
      USING ERRCODE = 'integrity_constraint_violation';
  END IF;
  IF TG_TABLE_NAME = 'documents' AND OLD.state = 'FAILED'
     AND NEW.state NOT IN ('FAILED', 'DELETED', 'QUEUED') THEN
    RAISE EXCEPTION 'a FAILED document may only be re-queued'
      USING ERRCODE = 'integrity_constraint_violation';
  END IF;
  RETURN NEW;
END
$function$;

REVOKE ALL ON FUNCTION nexora.guard_knowledge_terminal_transition() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION nexora.guard_knowledge_terminal_transition() TO nexora_runtime;

CREATE TRIGGER knowledge_bases_guard_terminal
BEFORE UPDATE ON nexora.knowledge_bases
FOR EACH ROW
EXECUTE FUNCTION nexora.guard_knowledge_terminal_transition();

CREATE TRIGGER documents_guard_terminal
BEFORE UPDATE ON nexora.documents
FOR EACH ROW
EXECUTE FUNCTION nexora.guard_knowledge_terminal_transition();

CREATE TRIGGER chat_sessions_guard_terminal
BEFORE UPDATE ON nexora.chat_sessions
FOR EACH ROW
EXECUTE FUNCTION nexora.guard_knowledge_terminal_transition();

CREATE TRIGGER chat_messages_guard_terminal
BEFORE UPDATE ON nexora.chat_messages
FOR EACH ROW
EXECUTE FUNCTION nexora.guard_knowledge_terminal_transition();

-- A document job may not mutate once its parent document is DELETED.
CREATE FUNCTION nexora.guard_document_job_parent_state()
RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = pg_catalog, nexora
AS $function$
BEGIN
  IF EXISTS (
    SELECT 1 FROM nexora.documents
    WHERE id = NEW.document_id
      AND organization_id = NEW.organization_id
      AND state = 'DELETED'
  ) THEN
    RAISE EXCEPTION 'a document job cannot mutate a DELETED document'
      USING ERRCODE = 'integrity_constraint_violation';
  END IF;
  RETURN NEW;
END
$function$;

REVOKE ALL ON FUNCTION nexora.guard_document_job_parent_state() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION nexora.guard_document_job_parent_state() TO nexora_runtime;

CREATE TRIGGER document_jobs_guard_parent
BEFORE UPDATE ON nexora.document_jobs
FOR EACH ROW
EXECUTE FUNCTION nexora.guard_document_job_parent_state();

-- Version maintenance on lifecycle tables, matching the M2 pattern.
CREATE TRIGGER knowledge_bases_advance_version
BEFORE UPDATE ON nexora.knowledge_bases
FOR EACH ROW
EXECUTE FUNCTION nexora.advance_row_version();

CREATE TRIGGER documents_advance_version
BEFORE UPDATE ON nexora.documents
FOR EACH ROW
EXECUTE FUNCTION nexora.advance_row_version();

CREATE TRIGGER document_jobs_advance_version
BEFORE UPDATE ON nexora.document_jobs
FOR EACH ROW
EXECUTE FUNCTION nexora.advance_row_version();

CREATE TRIGGER chunks_advance_version
BEFORE UPDATE ON nexora.chunks
FOR EACH ROW
EXECUTE FUNCTION nexora.advance_row_version();

CREATE TRIGGER chat_sessions_advance_version
BEFORE UPDATE ON nexora.chat_sessions
FOR EACH ROW
EXECUTE FUNCTION nexora.advance_row_version();

CREATE TRIGGER chat_messages_advance_version
BEFORE UPDATE ON nexora.chat_messages
FOR EACH ROW
EXECUTE FUNCTION nexora.advance_row_version();

CREATE TRIGGER retrieval_runs_advance_version
BEFORE UPDATE ON nexora.retrieval_runs
FOR EACH ROW
EXECUTE FUNCTION nexora.advance_row_version();

-- Enum hygiene: keep the M4 vocabulary out of PUBLIC and the Data API roles.
DO $$
DECLARE
  target text;
  enum_type text;
BEGIN
  FOREACH enum_type IN ARRAY ARRAY[
    'knowledge_base_state', 'document_state', 'document_job_state', 'chunk_state',
    'chat_session_state', 'chat_message_role', 'chat_message_state', 'retrieval_run_outcome'
  ] LOOP
    EXECUTE format('REVOKE USAGE ON TYPE nexora.%I FROM PUBLIC', enum_type);
    FOREACH target IN ARRAY ARRAY['anon', 'authenticated', 'service_role'] LOOP
      IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = target) THEN
        EXECUTE format('REVOKE USAGE ON TYPE nexora.%I FROM %I', enum_type, target);
      END IF;
    END LOOP;
  END LOOP;
END
$$;

-- Vector plane: application-owned vector rows with model/dimension provenance.
-- The extension itself is provisioned by the platform operator (managed
-- Supabase or the disposable verify stack), never by this application-owned
-- migration; V024 only requires it to already exist and records the observed
-- installed version in the verify fixture, never by SQL pinning.

CREATE TYPE nexora.vector_state AS ENUM (
  'ACTIVE',
  'SUPERSEDED',
  'DELETED'
);

CREATE TABLE rag.chunk_vectors (
  id uuid PRIMARY KEY,
  chunk_id uuid NOT NULL,
  document_id uuid NOT NULL,
  organization_id uuid NOT NULL,
  knowledge_base_id uuid NOT NULL,
  model_id text NOT NULL CHECK (char_length(model_id) BETWEEN 1 AND 200),
  model_revision text NOT NULL CHECK (char_length(model_revision) BETWEEN 1 AND 200),
  dimensions integer NOT NULL CHECK (dimensions > 0),
  embedding vector(1024) NOT NULL,
  sha256 text NOT NULL CHECK (sha256 ~ '^[a-f0-9]{64}$'),
  state nexora.vector_state NOT NULL DEFAULT 'ACTIVE',
  created_at timestamptz NOT NULL DEFAULT transaction_timestamp(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  CONSTRAINT chunk_vectors_chunk_fk FOREIGN KEY (chunk_id, organization_id)
    REFERENCES nexora.chunks (id, organization_id),
  CONSTRAINT chunk_vectors_tenant_key UNIQUE (organization_id, id),
  CONSTRAINT chunk_vectors_dimension_check CHECK (dimensions = 1024)
);

CREATE INDEX chunk_vectors_active_idx ON rag.chunk_vectors (document_id, organization_id) WHERE state = 'ACTIVE';

ALTER TABLE rag.chunk_vectors ENABLE ROW LEVEL SECURITY;
ALTER TABLE rag.chunk_vectors FORCE ROW LEVEL SECURITY;

REVOKE USAGE ON TYPE nexora.vector_state FROM PUBLIC;
REVOKE ALL ON TABLE rag.chunk_vectors FROM PUBLIC;

GRANT SELECT, INSERT, UPDATE ON rag.chunk_vectors TO nexora_runtime;
GRANT USAGE ON SCHEMA rag TO nexora_runtime;

-- Tenant-scoped vector access mirrors the chunk policy: ACTIVE vectors for
-- knowledge.read retrieval; managers additionally see SUPERSEDED/DELETED rows
-- so supersede/delete UPDATEs keep their NEW row visible under forced RLS.
CREATE POLICY chunk_vectors_select_tenant
ON rag.chunk_vectors FOR SELECT TO nexora_runtime
USING (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND (
    (state = 'ACTIVE' AND nexora.knowledge_current_tenant_has_permission('knowledge.read'))
    OR nexora.knowledge_current_tenant_has_permission('knowledge.manage')
  )
);

CREATE POLICY chunk_vectors_insert_tenant
ON rag.chunk_vectors FOR INSERT TO nexora_runtime
WITH CHECK (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.knowledge_current_tenant_has_permission('knowledge.manage')
);

CREATE POLICY chunk_vectors_update_tenant
ON rag.chunk_vectors FOR UPDATE TO nexora_runtime
USING (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.knowledge_current_tenant_has_permission('knowledge.manage')
)
WITH CHECK (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.knowledge_current_tenant_has_permission('knowledge.manage')
);

CREATE TRIGGER chunk_vectors_guard_terminal
BEFORE UPDATE ON rag.chunk_vectors
FOR EACH ROW
EXECUTE FUNCTION nexora.guard_knowledge_terminal_transition();

CREATE TRIGGER chunk_vectors_advance_version
BEFORE UPDATE ON rag.chunk_vectors
FOR EACH ROW
EXECUTE FUNCTION nexora.advance_row_version();

COMMENT ON TABLE rag.chunk_vectors IS
  'Application-owned vector rows. The vector extension is operator-provisioned before V024 applies; the verify fixture records the observed installed version and proves a representative similarity query, and a version outside the application-tested range blocks acceptance.';

COMMIT;
