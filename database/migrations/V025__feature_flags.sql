-- M5-DB01: tenant-scoped feature flags with deterministic evaluation.
-- Flags are tenant-bound, server-derived, and evaluated with consistent
-- hashing so a subject always sees the same variant for a given flag.

BEGIN;
SET LOCAL ROLE nexora_migrator;
SET LOCAL search_path = pg_catalog, nexora, public;

CREATE TABLE nexora.feature_flags (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     uuid NOT NULL REFERENCES nexora.tenants(id) ON DELETE CASCADE,
    flag_key      text NOT NULL,
    enabled       boolean NOT NULL DEFAULT false,
    -- rollout_percentage: 0..100, deterministic per subject via hash
    rollout_percentage integer NOT NULL DEFAULT 0
                      CHECK (rollout_percentage BETWEEN 0 AND 100),
    -- rules: optional JSON targeting rules (audience, segment, etc.)
    rules         jsonb NOT NULL DEFAULT '{}'::jsonb,
    description   text,
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, flag_key)
);

CREATE INDEX idx_feature_flags_tenant_key
    ON nexora.feature_flags (tenant_id, flag_key);

ALTER TABLE nexora.feature_flags ENABLE ROW LEVEL SECURITY;

CREATE POLICY feature_flags_tenant_isolation ON nexora.feature_flags
    USING (tenant_id = current_setting('nexora.tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('nexora.tenant_id')::uuid);

COMMENT ON TABLE nexora.feature_flags IS
    'Tenant-scoped feature flags with deterministic rollout evaluation.';

COMMIT;
