-- M5-DB01: product analytics events with tenant isolation.
-- Events are append-only, tenant-scoped, and support both structured
-- analytics queries and the M3 event spine for real-time fan-out.

BEGIN;
SET LOCAL ROLE nexora_migrator;
SET LOCAL search_path = pg_catalog, nexora, public;

CREATE TABLE nexora.analytics_events (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     uuid NOT NULL REFERENCES nexora.tenants(id) ON DELETE CASCADE,
    -- subject: the actor (user id) who triggered the event; nullable for anonymous
    subject_id    uuid REFERENCES nexora.users(id) ON DELETE SET NULL,
    event_type    text NOT NULL,
    -- resource: optional typed resource reference (page, document, flag, etc.)
    resource_type text,
    resource_id   uuid,
    -- properties: bounded JSON payload for event-specific data
    properties    jsonb NOT NULL DEFAULT '{}'::jsonb,
    -- client_context: sanitized client context (page path, referrer bucket)
    client_context jsonb NOT NULL DEFAULT '{}'::jsonb,
    recorded_at   timestamptz NOT NULL DEFAULT now(),
    -- idempotency_key: optional dedupe key for replay-safe ingestion
    idempotency_key text
);

CREATE INDEX idx_analytics_tenant_time
    ON nexora.analytics_events (tenant_id, recorded_at DESC);
CREATE INDEX idx_analytics_tenant_type_time
    ON nexora.analytics_events (tenant_id, event_type, recorded_at DESC);
CREATE INDEX idx_analytics_idempotency
    ON nexora.analytics_events (tenant_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

ALTER TABLE nexora.analytics_events ENABLE ROW LEVEL SECURITY;

CREATE POLICY analytics_tenant_isolation ON nexora.analytics_events
    USING (tenant_id = current_setting('nexora.tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('nexora.tenant_id')::uuid);

COMMENT ON TABLE nexora.analytics_events IS
    'Append-only tenant-scoped product analytics events. Subject to retention policy.';

COMMIT;
