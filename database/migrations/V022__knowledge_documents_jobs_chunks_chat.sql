-- M4-DB01: tenant-scoped knowledge bases, documents, durable jobs, chunks,
-- chat sessions/messages and retrieval-run traces. This migration creates no
-- managed-schema object and no provider credential.

BEGIN;
SET LOCAL ROLE nexora_migrator;

CREATE TYPE nexora.knowledge_base_state AS ENUM (
  'ACTIVE',
  'ARCHIVED',
  'DELETED'
);

CREATE TYPE nexora.document_state AS ENUM (
  'UPLOADED',
  'QUEUED',
  'EXTRACTING',
  'NORMALIZING',
  'CHUNKING',
  'EMBEDDING',
  'INDEXING',
  'READY',
  'FAILED',
  'CANCELLED',
  'DELETED'
);

CREATE TYPE nexora.document_job_state AS ENUM (
  'QUEUED',
  'RUNNING',
  'SUCCEEDED',
  'FAILED',
  'CANCELLED'
);

CREATE TYPE nexora.chunk_state AS ENUM (
  'ACTIVE',
  'SUPERSEDED',
  'DELETED'
);

CREATE TYPE nexora.chat_session_state AS ENUM (
  'ACTIVE',
  'ARCHIVED',
  'DELETED'
);

CREATE TYPE nexora.chat_message_role AS ENUM (
  'user',
  'assistant',
  'system'
);

CREATE TYPE nexora.chat_message_state AS ENUM (
  'DRAFT',
  'STREAMING',
  'COMPLETED',
  'CANCELLED',
  'FAILED',
  'DELETED'
);

CREATE TYPE nexora.retrieval_run_outcome AS ENUM (
  'ANSWERED',
  'NO_ANSWER',
  'LOW_CONFIDENCE',
  'CANCELLED',
  'FAILED'
);

CREATE TABLE nexora.knowledge_bases (
  id uuid PRIMARY KEY,
  organization_id uuid NOT NULL,
  name text NOT NULL CHECK (char_length(name) BETWEEN 1 AND 200),
  description text NOT NULL DEFAULT '' CHECK (char_length(description) <= 2000),
  state nexora.knowledge_base_state NOT NULL DEFAULT 'ACTIVE',
  created_by_subject_id uuid NOT NULL,
  created_at timestamptz NOT NULL DEFAULT transaction_timestamp(),
  updated_at timestamptz NOT NULL DEFAULT transaction_timestamp(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  CONSTRAINT knowledge_bases_tenant_key UNIQUE (organization_id, id)
);

CREATE TABLE nexora.documents (
  id uuid PRIMARY KEY,
  knowledge_base_id uuid NOT NULL,
  organization_id uuid NOT NULL,
  original_name text NOT NULL CHECK (char_length(original_name) BETWEEN 1 AND 255),
  stored_object_key text NOT NULL CHECK (char_length(stored_object_key) BETWEEN 1 AND 512),
  content_type text NOT NULL CHECK (content_type IN ('application/pdf', 'text/markdown', 'text/plain')),
  byte_size bigint NOT NULL CHECK (byte_size BETWEEN 0 AND 52428800),
  sha256 text NOT NULL CHECK (sha256 ~ '^[a-f0-9]{64}$'),
  state nexora.document_state NOT NULL DEFAULT 'UPLOADED',
  uploaded_by_subject_id uuid NOT NULL,
  created_at timestamptz NOT NULL DEFAULT transaction_timestamp(),
  updated_at timestamptz NOT NULL DEFAULT transaction_timestamp(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  CONSTRAINT documents_knowledge_base_fk FOREIGN KEY (knowledge_base_id, organization_id)
    REFERENCES nexora.knowledge_bases (id, organization_id),
  CONSTRAINT documents_tenant_key UNIQUE (organization_id, id),
  CONSTRAINT documents_dedup_key UNIQUE (organization_id, sha256, original_name)
);

CREATE TABLE nexora.document_jobs (
  id uuid PRIMARY KEY,
  document_id uuid NOT NULL,
  organization_id uuid NOT NULL,
  state nexora.document_job_state NOT NULL DEFAULT 'QUEUED',
  attempt_count integer NOT NULL DEFAULT 0 CHECK (attempt_count BETWEEN 0 AND 5),
  max_attempts integer NOT NULL DEFAULT 5 CHECK (max_attempts BETWEEN 1 AND 5),
  last_error_code text CHECK (last_error_code IS NULL OR last_error_code ~ '^[A-Z][A-Z0-9_]{1,63}$'),
  created_at timestamptz NOT NULL DEFAULT transaction_timestamp(),
  updated_at timestamptz NOT NULL DEFAULT transaction_timestamp(),
  heartbeat_at timestamptz,
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  CONSTRAINT document_jobs_document_fk FOREIGN KEY (document_id, organization_id)
    REFERENCES nexora.documents (id, organization_id),
  CONSTRAINT document_jobs_tenant_key UNIQUE (organization_id, id),
  CONSTRAINT document_jobs_shape_check CHECK (
    (state = 'RUNNING') = (heartbeat_at IS NOT NULL)
  ),
  CONSTRAINT document_jobs_terminal_shape_check CHECK (
    state NOT IN ('SUCCEEDED', 'FAILED', 'CANCELLED')
    OR last_error_code IS NULL
  )
);

CREATE TABLE nexora.chunks (
  id uuid PRIMARY KEY,
  document_id uuid NOT NULL,
  organization_id uuid NOT NULL,
  knowledge_base_id uuid NOT NULL,
  chunk_index integer NOT NULL CHECK (chunk_index >= 0),
  text text NOT NULL CHECK (char_length(text) BETWEEN 1 AND 20000),
  token_count integer NOT NULL CHECK (token_count BETWEEN 1 AND 1500),
  source_page_start integer CHECK (source_page_start IS NULL OR source_page_start >= 0),
  source_page_end integer CHECK (source_page_end IS NULL OR source_page_end >= source_page_start),
  source_section text NOT NULL DEFAULT '',
  sha256 text NOT NULL CHECK (sha256 ~ '^[a-f0-9]{64}$'),
  chunk_strategy_version text NOT NULL CHECK (chunk_strategy_version ~ '^nexora-chunk-v[0-9]+$'),
  state nexora.chunk_state NOT NULL DEFAULT 'ACTIVE',
  created_at timestamptz NOT NULL DEFAULT transaction_timestamp(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  CONSTRAINT chunks_document_fk FOREIGN KEY (document_id, organization_id)
    REFERENCES nexora.documents (id, organization_id),
  CONSTRAINT chunks_tenant_key UNIQUE (organization_id, id),
  CONSTRAINT chunks_position_key UNIQUE (organization_id, document_id, chunk_strategy_version, chunk_index)
);

CREATE TABLE nexora.chat_sessions (
  id uuid PRIMARY KEY,
  organization_id uuid NOT NULL,
  subject_id uuid NOT NULL,
  title text NOT NULL DEFAULT '' CHECK (char_length(title) <= 200),
  state nexora.chat_session_state NOT NULL DEFAULT 'ACTIVE',
  created_at timestamptz NOT NULL DEFAULT transaction_timestamp(),
  updated_at timestamptz NOT NULL DEFAULT transaction_timestamp(),
  deleted_at timestamptz,
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  CONSTRAINT chat_sessions_tenant_key UNIQUE (organization_id, id),
  CONSTRAINT chat_sessions_shape_check CHECK (
    (state = 'DELETED') = (deleted_at IS NOT NULL)
  )
);

CREATE TABLE nexora.chat_messages (
  id uuid PRIMARY KEY,
  session_id uuid NOT NULL,
  organization_id uuid NOT NULL,
  subject_id uuid NOT NULL,
  client_message_id text NOT NULL CHECK (char_length(client_message_id) BETWEEN 1 AND 128),
  client_message_id_digest text NOT NULL CHECK (client_message_id_digest ~ '^sha256:[a-f0-9]{64}$'),
  role nexora.chat_message_role NOT NULL,
  state nexora.chat_message_state NOT NULL DEFAULT 'DRAFT',
  revision integer NOT NULL DEFAULT 1 CHECK (revision > 0),
  parent_message_id uuid,
  content text NOT NULL DEFAULT '' CHECK (char_length(content) <= 100000),
  created_at timestamptz NOT NULL DEFAULT transaction_timestamp(),
  updated_at timestamptz NOT NULL DEFAULT transaction_timestamp(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  CONSTRAINT chat_messages_session_fk FOREIGN KEY (session_id, organization_id)
    REFERENCES nexora.chat_sessions (id, organization_id),
  CONSTRAINT chat_messages_parent_fk FOREIGN KEY (parent_message_id, organization_id)
    REFERENCES nexora.chat_messages (id, organization_id),
  CONSTRAINT chat_messages_tenant_key UNIQUE (organization_id, id),
  CONSTRAINT chat_messages_idempotency_key UNIQUE (organization_id, session_id, subject_id, client_message_id_digest),
  CONSTRAINT chat_messages_shape_check CHECK (
    state <> 'DELETED' OR content = ''
  )
);

CREATE TABLE nexora.retrieval_runs (
  id uuid PRIMARY KEY,
  session_id uuid,
  organization_id uuid NOT NULL,
  subject_id uuid NOT NULL,
  query_hash text NOT NULL CHECK (query_hash ~ '^sha256:[a-f0-9]{64}$'),
  corpus_version text NOT NULL,
  model_id text NOT NULL,
  model_revision text NOT NULL,
  candidate_ids jsonb NOT NULL DEFAULT '[]'::jsonb CHECK (jsonb_typeof(candidate_ids) = 'array'),
  selected_chunk_ids jsonb NOT NULL DEFAULT '[]'::jsonb CHECK (jsonb_typeof(selected_chunk_ids) = 'array'),
  outcome nexora.retrieval_run_outcome NOT NULL,
  latency_ms bigint NOT NULL CHECK (latency_ms >= 0),
  token_count integer NOT NULL DEFAULT 0 CHECK (token_count >= 0),
  created_at timestamptz NOT NULL DEFAULT transaction_timestamp(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  CONSTRAINT retrieval_runs_session_fk FOREIGN KEY (session_id, organization_id)
    REFERENCES nexora.chat_sessions (id, organization_id),
  CONSTRAINT retrieval_runs_tenant_key UNIQUE (organization_id, id)
);

CREATE INDEX documents_knowledge_base_idx ON nexora.documents (knowledge_base_id, organization_id) WHERE state <> 'DELETED';
CREATE INDEX document_jobs_claim_idx ON nexora.document_jobs (state, heartbeat_at) WHERE state = 'RUNNING';
CREATE INDEX chunks_document_idx ON nexora.chunks (document_id, organization_id) WHERE state = 'ACTIVE';
CREATE INDEX chat_messages_session_idx ON nexora.chat_messages (session_id, organization_id, created_at) WHERE state <> 'DELETED';
CREATE INDEX retrieval_runs_session_idx ON nexora.retrieval_runs (session_id, organization_id, created_at) WHERE session_id IS NOT NULL;

COMMIT;
