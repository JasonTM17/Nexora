\set ON_ERROR_STOP on

DO $$
DECLARE
  application_schema text;
  api_role text;
BEGIN
  FOREACH application_schema IN ARRAY ARRAY['nexora', 'rag', 'audit']
  LOOP
    IF NOT EXISTS (
      SELECT 1
      FROM pg_namespace namespace
      JOIN pg_roles owner_role ON owner_role.oid = namespace.nspowner
      WHERE namespace.nspname = application_schema
        AND owner_role.rolname = 'nexora_migrator'
    ) THEN
      RAISE EXCEPTION 'schema % must be owned by nexora_migrator', application_schema;
    END IF;

    IF NOT has_schema_privilege('nexora_runtime', application_schema, 'USAGE')
       OR has_schema_privilege('nexora_runtime', application_schema, 'CREATE') THEN
      RAISE EXCEPTION 'nexora_runtime schema privilege boundary failed for %', application_schema;
    END IF;

    IF has_schema_privilege('public', application_schema, 'USAGE')
       OR has_schema_privilege('public', application_schema, 'CREATE') THEN
      RAISE EXCEPTION 'PUBLIC must not access application schema %', application_schema;
    END IF;

    FOREACH api_role IN ARRAY ARRAY['anon', 'authenticated', 'service_role']
    LOOP
      IF has_schema_privilege(api_role, application_schema, 'USAGE')
         OR has_schema_privilege(api_role, application_schema, 'CREATE') THEN
        RAISE EXCEPTION 'Data API role % must not access application schema %', api_role, application_schema;
      END IF;
    END LOOP;
  END LOOP;

  IF EXISTS (
    SELECT 1
    FROM pg_roles
    WHERE rolname IN ('nexora_migrator', 'nexora_runtime')
      AND (rolsuper OR rolcreaterole OR rolcreatedb OR rolreplication OR rolbypassrls)
  ) THEN
    RAISE EXCEPTION 'Nexora roles must remain least-privilege and NOBYPASSRLS';
  END IF;
END
$$;

SELECT
  'foundation verification passed' AS result,
  current_database() AS database_name,
  current_setting('server_version') AS postgresql_version;
