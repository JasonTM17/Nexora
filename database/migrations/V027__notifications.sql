-- M5-DB01: tenant-scoped notifications with multi-channel delivery.
-- Notifications are user-bound within a tenant, support read state
-- tracking, and fan out through the M3 event spine for real-time delivery.

BEGIN;
SET LOCAL ROLE nexora_migrator;
SET LOCAL search_path = pg_catalog, nexora, public;

CREATE TABLE nexora.notifications (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     uuid NOT NULL REFERENCES nexora.tenants(id) ON DELETE CASCADE,
    user_id       uuid NOT NULL REFERENCES nexora.users(id) ON DELETE CASCADE,
    notification_type text NOT NULL,
    -- priority: low, normal, high, urgent
    priority      text NOT NULL DEFAULT 'normal'
                  CHECK (priority IN ('low', 'normal', 'high', 'urgent')),
    title         text NOT NULL,
    body          text NOT NULL DEFAULT '',
    -- action_url: optional deep link for the notification
    action_url    text,
    -- metadata: bounded JSON for type-specific payload
    metadata      jsonb NOT NULL DEFAULT '{}'::jsonb,
    read_at       timestamptz,
    created_at    timestamptz NOT NULL DEFAULT now(),
    -- ttl: auto-purge after this timestamp
    ttl           timestamptz NOT NULL DEFAULT now() + interval '30 days'
);

CREATE INDEX idx_notifications_user_unread
    ON nexora.notifications (tenant_id, user_id, created_at DESC)
    WHERE read_at IS NULL;
CREATE INDEX idx_notifications_user_time
    ON nexora.notifications (tenant_id, user_id, created_at DESC);
CREATE INDEX idx_notifications_ttl
    ON nexora.notifications (ttl);

ALTER TABLE nexora.notifications ENABLE ROW LEVEL SECURITY;

CREATE POLICY notifications_tenant_isolation ON nexora.notifications
    USING (tenant_id = current_setting('nexora.tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('nexora.tenant_id')::uuid);

-- Users can only access their own notifications within the tenant
CREATE POLICY notifications_user_scoping ON nexora.notifications
    USING (user_id = current_setting('nexora.subject_id')::uuid)
    WITH CHECK (user_id = current_setting('nexora.subject_id')::uuid);

COMMENT ON TABLE nexora.notifications IS
    'Tenant- and user-scoped notifications with TTL-based auto-purge.';

COMMIT;
