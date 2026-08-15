-- M5-DB01: tenant-scoped A/B experiments with deterministic variant assignment.
-- Experiments share the feature flag hashing spine for stable per-subject
-- assignment without per-subject storage.

BEGIN;
SET LOCAL ROLE nexora_migrator;
SET LOCAL search_path = pg_catalog, nexora, public;

CREATE TABLE nexora.experiments (
    id                     uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id              uuid NOT NULL REFERENCES nexora.tenants(id) ON DELETE CASCADE,
    experiment_key         text NOT NULL,
    active                 boolean NOT NULL DEFAULT false,
    treatment_percentage   integer NOT NULL DEFAULT 50
                           CHECK (treatment_percentage BETWEEN 0 AND 100),
    description            text,
    created_at             timestamptz NOT NULL DEFAULT now(),
    updated_at             timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, experiment_key)
);

CREATE INDEX idx_experiments_tenant_key
    ON nexora.experiments (tenant_id, experiment_key);

ALTER TABLE nexora.experiments ENABLE ROW LEVEL SECURITY;

CREATE POLICY experiments_tenant_isolation ON nexora.experiments
    USING (tenant_id = current_setting('nexora.tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('nexora.tenant_id')::uuid);

COMMENT ON TABLE nexora.experiments IS
    'Tenant-scoped A/B experiments with deterministic variant assignment.';

COMMIT;
