-- M1-DB01 application-owned database baseline.
--
-- Flyway-style ordering is the only schema-history authority for nexora, rag,
-- and audit.  Do not add domain tables to public, auth, storage, or realtime.
-- Managed Supabase schemas are deliberately not referenced or modified here.

BEGIN;

-- These are group roles.  Deployment credentials may assume the migration role
-- through an approved, separately managed login, but the application connection
-- must use only nexora_runtime.  Neither role can bypass RLS.
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'nexora_migrator') THEN
    CREATE ROLE nexora_migrator NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
      NOINHERIT NOREPLICATION NOBYPASSRLS;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'nexora_runtime') THEN
    CREATE ROLE nexora_runtime NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
      NOINHERIT NOREPLICATION NOBYPASSRLS;
  END IF;
END
$$;

CREATE SCHEMA nexora AUTHORIZATION nexora_migrator;
CREATE SCHEMA rag AUTHORIZATION nexora_migrator;
CREATE SCHEMA audit AUTHORIZATION nexora_migrator;

COMMENT ON SCHEMA nexora IS
  'Nexora domain schema. M2 introduces tenant tables and must enable and force RLS on every tenant-scoped relation.';
COMMENT ON SCHEMA rag IS
  'Nexora RAG domain schema. M4 owns its tables, RLS policies, and extension compatibility evidence.';
COMMENT ON SCHEMA audit IS
  'Nexora audit domain schema. Future relations require explicit RLS and per-object runtime grants.';

-- A schema is not an API surface.  Explicitly deny the public role and, where
-- present in a managed target, every Data API/service role from application
-- schema use.  The runtime receives schema traversal only; future migrations
-- must grant table, sequence, and function rights per object after RLS review.
REVOKE ALL ON SCHEMA nexora, rag, audit FROM PUBLIC;
REVOKE ALL ON ALL TABLES IN SCHEMA nexora, rag, audit FROM PUBLIC;
REVOKE ALL ON ALL SEQUENCES IN SCHEMA nexora, rag, audit FROM PUBLIC;
REVOKE ALL ON ALL FUNCTIONS IN SCHEMA nexora, rag, audit FROM PUBLIC;

GRANT USAGE ON SCHEMA nexora, rag, audit TO nexora_runtime;
REVOKE CREATE ON SCHEMA nexora, rag, audit FROM nexora_runtime;

-- Default privileges close the historical automatic-grant path.  Each owning
-- migration must make a deliberate per-object grant and create/force RLS before
-- a runtime role can touch a tenant-scoped table.
ALTER DEFAULT PRIVILEGES FOR ROLE nexora_migrator IN SCHEMA nexora
  REVOKE ALL ON TABLES FROM PUBLIC;
ALTER DEFAULT PRIVILEGES FOR ROLE nexora_migrator IN SCHEMA rag
  REVOKE ALL ON TABLES FROM PUBLIC;
ALTER DEFAULT PRIVILEGES FOR ROLE nexora_migrator IN SCHEMA audit
  REVOKE ALL ON TABLES FROM PUBLIC;
ALTER DEFAULT PRIVILEGES FOR ROLE nexora_migrator IN SCHEMA nexora
  REVOKE ALL ON SEQUENCES FROM PUBLIC;
ALTER DEFAULT PRIVILEGES FOR ROLE nexora_migrator IN SCHEMA rag
  REVOKE ALL ON SEQUENCES FROM PUBLIC;
ALTER DEFAULT PRIVILEGES FOR ROLE nexora_migrator IN SCHEMA audit
  REVOKE ALL ON SEQUENCES FROM PUBLIC;
ALTER DEFAULT PRIVILEGES FOR ROLE nexora_migrator IN SCHEMA nexora
  REVOKE EXECUTE ON FUNCTIONS FROM PUBLIC;
ALTER DEFAULT PRIVILEGES FOR ROLE nexora_migrator IN SCHEMA rag
  REVOKE EXECUTE ON FUNCTIONS FROM PUBLIC;
ALTER DEFAULT PRIVILEGES FOR ROLE nexora_migrator IN SCHEMA audit
  REVOKE EXECUTE ON FUNCTIONS FROM PUBLIC;

DO $$
DECLARE
  api_role text;
  application_schema text;
BEGIN
  FOREACH api_role IN ARRAY ARRAY['anon', 'authenticated', 'service_role']
  LOOP
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = api_role) THEN
      FOREACH application_schema IN ARRAY ARRAY['nexora', 'rag', 'audit']
      LOOP
        EXECUTE format('REVOKE ALL ON SCHEMA %I FROM %I', application_schema, api_role);
        EXECUTE format('REVOKE ALL ON ALL TABLES IN SCHEMA %I FROM %I', application_schema, api_role);
        EXECUTE format('REVOKE ALL ON ALL SEQUENCES IN SCHEMA %I FROM %I', application_schema, api_role);
        EXECUTE format('REVOKE ALL ON ALL FUNCTIONS IN SCHEMA %I FROM %I', application_schema, api_role);
        EXECUTE format(
          'ALTER DEFAULT PRIVILEGES FOR ROLE nexora_migrator IN SCHEMA %I REVOKE ALL ON TABLES FROM %I',
          application_schema,
          api_role
        );
        EXECUTE format(
          'ALTER DEFAULT PRIVILEGES FOR ROLE nexora_migrator IN SCHEMA %I REVOKE ALL ON SEQUENCES FROM %I',
          application_schema,
          api_role
        );
        EXECUTE format(
          'ALTER DEFAULT PRIVILEGES FOR ROLE nexora_migrator IN SCHEMA %I REVOKE EXECUTE ON FUNCTIONS FROM %I',
          application_schema,
          api_role
        );
      END LOOP;
    END IF;
  END LOOP;
END
$$;

COMMIT;
