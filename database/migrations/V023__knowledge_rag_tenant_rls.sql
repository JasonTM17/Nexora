-- M4-DB01 RLS access for V022. Every policy is scoped to the transaction-local
-- tenant context plus the frozen knowledge permission pair. Retrieval and chat
-- use knowledge.read; ingestion, mutation and deletion use knowledge.manage.

BEGIN;
SET LOCAL ROLE nexora_migrator;

CREATE FUNCTION nexora.knowledge_current_tenant_has_permission(
  required_permission nexora.tenant_permission
)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY INVOKER
SET search_path = pg_catalog, nexora
AS $function$
  SELECT EXISTS (
    SELECT 1
    FROM nexora.membership_authorizations AS actor
    JOIN nexora.tenant_role_permissions AS granted
      ON granted.tenant_role = actor.tenant_role
     AND granted.permission = required_permission
    WHERE actor.membership_id = NULLIF(current_setting('nexora.membership_id', true), '')::uuid
      AND actor.organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
      AND actor.subject_id = NULLIF(current_setting('nexora.subject_id', true), '')::uuid
      AND actor.status = 'ACTIVE'
  )
$function$;

REVOKE ALL ON FUNCTION nexora.knowledge_current_tenant_has_permission(nexora.tenant_permission) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION nexora.knowledge_current_tenant_has_permission(nexora.tenant_permission) TO nexora_runtime;

ALTER TABLE nexora.knowledge_bases ENABLE ROW LEVEL SECURITY;
ALTER TABLE nexora.documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE nexora.document_jobs ENABLE ROW LEVEL SECURITY;
ALTER TABLE nexora.chunks ENABLE ROW LEVEL SECURITY;
ALTER TABLE nexora.chat_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE nexora.chat_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE nexora.retrieval_runs ENABLE ROW LEVEL SECURITY;

ALTER TABLE nexora.knowledge_bases FORCE ROW LEVEL SECURITY;
ALTER TABLE nexora.documents FORCE ROW LEVEL SECURITY;
ALTER TABLE nexora.document_jobs FORCE ROW LEVEL SECURITY;
ALTER TABLE nexora.chunks FORCE ROW LEVEL SECURITY;
ALTER TABLE nexora.chat_sessions FORCE ROW LEVEL SECURITY;
ALTER TABLE nexora.chat_messages FORCE ROW LEVEL SECURITY;
ALTER TABLE nexora.retrieval_runs FORCE ROW LEVEL SECURITY;

GRANT USAGE ON TYPE nexora.knowledge_base_state TO nexora_runtime;
GRANT USAGE ON TYPE nexora.document_state TO nexora_runtime;
GRANT USAGE ON TYPE nexora.document_job_state TO nexora_runtime;
GRANT USAGE ON TYPE nexora.chunk_state TO nexora_runtime;
GRANT USAGE ON TYPE nexora.chat_session_state TO nexora_runtime;
GRANT USAGE ON TYPE nexora.chat_message_role TO nexora_runtime;
GRANT USAGE ON TYPE nexora.chat_message_state TO nexora_runtime;
GRANT USAGE ON TYPE nexora.retrieval_run_outcome TO nexora_runtime;

GRANT SELECT, INSERT, UPDATE ON nexora.knowledge_bases TO nexora_runtime;
GRANT SELECT, INSERT, UPDATE ON nexora.documents TO nexora_runtime;
GRANT SELECT, INSERT, UPDATE ON nexora.document_jobs TO nexora_runtime;
GRANT SELECT, INSERT, UPDATE ON nexora.chunks TO nexora_runtime;
GRANT SELECT, INSERT, UPDATE ON nexora.chat_sessions TO nexora_runtime;
GRANT SELECT, INSERT, UPDATE ON nexora.chat_messages TO nexora_runtime;
GRANT SELECT, INSERT ON nexora.retrieval_runs TO nexora_runtime;

-- Knowledge bases: read for knowledge.read; create/update/archive/delete for
-- knowledge.manage. A DELETED knowledge base is never readable.
CREATE POLICY knowledge_bases_select_tenant
ON nexora.knowledge_bases FOR SELECT TO nexora_runtime
USING (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND state <> 'DELETED'
  AND nexora.knowledge_current_tenant_has_permission('knowledge.read')
);

CREATE POLICY knowledge_bases_insert_tenant
ON nexora.knowledge_bases FOR INSERT TO nexora_runtime
WITH CHECK (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.knowledge_current_tenant_has_permission('knowledge.manage')
);

CREATE POLICY knowledge_bases_update_tenant
ON nexora.knowledge_bases FOR UPDATE TO nexora_runtime
USING (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.knowledge_current_tenant_has_permission('knowledge.manage')
)
WITH CHECK (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.knowledge_current_tenant_has_permission('knowledge.manage')
);

-- Documents: knowledge.read for listing/reading, knowledge.manage for
-- upload/state mutations. A DELETED document is never readable through this
-- policy, so retrieval eligibility dies with the state change.
CREATE POLICY documents_select_tenant
ON nexora.documents FOR SELECT TO nexora_runtime
USING (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND state <> 'DELETED'
  AND nexora.knowledge_current_tenant_has_permission('knowledge.read')
);

CREATE POLICY documents_insert_tenant
ON nexora.documents FOR INSERT TO nexora_runtime
WITH CHECK (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.knowledge_current_tenant_has_permission('knowledge.manage')
);

CREATE POLICY documents_update_tenant
ON nexora.documents FOR UPDATE TO nexora_runtime
USING (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.knowledge_current_tenant_has_permission('knowledge.manage')
)
WITH CHECK (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.knowledge_current_tenant_has_permission('knowledge.manage')
);

-- Jobs are an internal worker surface but still tenant-scoped; the runtime
-- worker executes them under knowledge.manage while progress reads use read.
CREATE POLICY document_jobs_select_tenant
ON nexora.document_jobs FOR SELECT TO nexora_runtime
USING (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.knowledge_current_tenant_has_permission('knowledge.read')
);

CREATE POLICY document_jobs_insert_tenant
ON nexora.document_jobs FOR INSERT TO nexora_runtime
WITH CHECK (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.knowledge_current_tenant_has_permission('knowledge.manage')
);

CREATE POLICY document_jobs_update_tenant
ON nexora.document_jobs FOR UPDATE TO nexora_runtime
USING (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.knowledge_current_tenant_has_permission('knowledge.manage')
)
WITH CHECK (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.knowledge_current_tenant_has_permission('knowledge.manage')
);

-- Chunks: readable only while ACTIVE and under knowledge.read; mutations
-- (supersede/delete) need knowledge.manage.
CREATE POLICY chunks_select_tenant
ON nexora.chunks FOR SELECT TO nexora_runtime
USING (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND state = 'ACTIVE'
  AND nexora.knowledge_current_tenant_has_permission('knowledge.read')
);

CREATE POLICY chunks_insert_tenant
ON nexora.chunks FOR INSERT TO nexora_runtime
WITH CHECK (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.knowledge_current_tenant_has_permission('knowledge.manage')
);

CREATE POLICY chunks_update_tenant
ON nexora.chunks FOR UPDATE TO nexora_runtime
USING (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.knowledge_current_tenant_has_permission('knowledge.manage')
)
WITH CHECK (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND nexora.knowledge_current_tenant_has_permission('knowledge.manage')
);

-- Chat: a subject may read only its own ACTIVE sessions and non-DELETED
-- messages under knowledge.read; both owner and tenant context must match.
CREATE POLICY chat_sessions_select_tenant
ON nexora.chat_sessions FOR SELECT TO nexora_runtime
USING (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND subject_id = NULLIF(current_setting('nexora.subject_id', true), '')::uuid
  AND state <> 'DELETED'
  AND nexora.knowledge_current_tenant_has_permission('knowledge.read')
);

CREATE POLICY chat_sessions_insert_tenant
ON nexora.chat_sessions FOR INSERT TO nexora_runtime
WITH CHECK (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND subject_id = NULLIF(current_setting('nexora.subject_id', true), '')::uuid
  AND nexora.knowledge_current_tenant_has_permission('knowledge.read')
);

CREATE POLICY chat_sessions_update_tenant
ON nexora.chat_sessions FOR UPDATE TO nexora_runtime
USING (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND subject_id = NULLIF(current_setting('nexora.subject_id', true), '')::uuid
  AND nexora.knowledge_current_tenant_has_permission('knowledge.read')
)
WITH CHECK (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND subject_id = NULLIF(current_setting('nexora.subject_id', true), '')::uuid
  AND nexora.knowledge_current_tenant_has_permission('knowledge.read')
);

CREATE POLICY chat_messages_select_tenant
ON nexora.chat_messages FOR SELECT TO nexora_runtime
USING (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND subject_id = NULLIF(current_setting('nexora.subject_id', true), '')::uuid
  AND state <> 'DELETED'
  AND nexora.knowledge_current_tenant_has_permission('knowledge.read')
);

CREATE POLICY chat_messages_insert_tenant
ON nexora.chat_messages FOR INSERT TO nexora_runtime
WITH CHECK (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND subject_id = NULLIF(current_setting('nexora.subject_id', true), '')::uuid
  AND nexora.knowledge_current_tenant_has_permission('knowledge.read')
);

CREATE POLICY chat_messages_update_tenant
ON nexora.chat_messages FOR UPDATE TO nexora_runtime
USING (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND subject_id = NULLIF(current_setting('nexora.subject_id', true), '')::uuid
  AND nexora.knowledge_current_tenant_has_permission('knowledge.read')
)
WITH CHECK (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND subject_id = NULLIF(current_setting('nexora.subject_id', true), '')::uuid
  AND nexora.knowledge_current_tenant_has_permission('knowledge.read')
);

-- Retrieval runs: subject-scoped reads only.
CREATE POLICY retrieval_runs_select_tenant
ON nexora.retrieval_runs FOR SELECT TO nexora_runtime
USING (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND subject_id = NULLIF(current_setting('nexora.subject_id', true), '')::uuid
  AND nexora.knowledge_current_tenant_has_permission('knowledge.read')
);

CREATE POLICY retrieval_runs_insert_tenant
ON nexora.retrieval_runs FOR INSERT TO nexora_runtime
WITH CHECK (
  organization_id = NULLIF(current_setting('nexora.organization_id', true), '')::uuid
  AND subject_id = NULLIF(current_setting('nexora.subject_id', true), '')::uuid
  AND nexora.knowledge_current_tenant_has_permission('knowledge.read')
);

COMMIT;
