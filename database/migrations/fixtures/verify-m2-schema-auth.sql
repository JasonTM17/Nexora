\set ON_ERROR_STOP on

-- This fixture is intentionally synthetic and runs only in the disposable
-- local PostgreSQL 17.5 container created by verify-m2-schema-auth.ps1.

DO $$
BEGIN
  IF current_setting('server_version') NOT LIKE '17.5%' THEN
    RAISE EXCEPTION 'M2-DB01 requires PostgreSQL 17.5 verification, observed %',
      current_setting('server_version');
  END IF;

  IF EXISTS (
    SELECT 1
    FROM pg_roles
    WHERE rolname = 'nexora_runtime'
      AND (rolsuper OR rolcreaterole OR rolcreatedb OR rolreplication OR rolbypassrls OR rolcanlogin)
  ) THEN
    RAISE EXCEPTION 'nexora_runtime must remain a non-login, non-owner, NOBYPASSRLS group role';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM pg_class relation
    JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
    JOIN pg_roles owner_role ON owner_role.oid = relation.relowner
    WHERE namespace.nspname = 'nexora'
      AND relation.relname IN (
        'profiles',
        'organizations',
        'memberships',
        'membership_authorizations',
        'tenant_role_permissions'
      )
      AND owner_role.rolname <> 'nexora_migrator'
  ) THEN
    RAISE EXCEPTION 'every M2 relation must be owned by nexora_migrator';
  END IF;

  IF (SELECT count(*) FROM pg_class relation
      JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
      WHERE namespace.nspname = 'nexora'
        AND relation.relname IN (
          'profiles',
          'organizations',
          'memberships',
          'membership_authorizations'
        )
        AND relation.relrowsecurity
        AND relation.relforcerowsecurity) <> 4 THEN
    RAISE EXCEPTION 'profiles, organizations, memberships, and membership_authorizations must enable and force RLS';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM pg_policy policy
    JOIN pg_class relation ON relation.oid = policy.polrelid
    JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
    WHERE namespace.nspname = 'nexora'
      AND relation.relname IN (
        'profiles',
        'organizations',
        'memberships',
        'membership_authorizations'
      )
      AND policy.polroles <> ARRAY[(SELECT oid FROM pg_roles WHERE rolname = 'nexora_runtime')]
  ) THEN
    RAISE EXCEPTION 'every M2 RLS policy must target nexora_runtime explicitly';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM pg_proc procedure
    JOIN pg_namespace namespace ON namespace.oid = procedure.pronamespace
    WHERE namespace.nspname = 'nexora'
      AND procedure.prosecdef
  ) THEN
    RAISE EXCEPTION 'M2-DB01 must not create SECURITY DEFINER functions';
  END IF;
END
$$;

DO $$
DECLARE
  api_role text;
  relation_name text;
BEGIN
  FOREACH api_role IN ARRAY ARRAY['anon', 'authenticated', 'service_role']
  LOOP
    IF has_schema_privilege(api_role, 'nexora', 'USAGE') THEN
      RAISE EXCEPTION 'Data API role % must not use nexora schema', api_role;
    END IF;

    FOREACH relation_name IN ARRAY ARRAY[
      'nexora.profiles',
      'nexora.organizations',
      'nexora.memberships',
      'nexora.membership_authorizations',
      'nexora.tenant_role_permissions'
    ]
    LOOP
      IF has_table_privilege(api_role, relation_name, 'SELECT,INSERT,UPDATE,DELETE,TRUNCATE,REFERENCES,TRIGGER') THEN
        RAISE EXCEPTION 'Data API role % must not access %', api_role, relation_name;
      END IF;
    END LOOP;

    IF has_type_privilege(api_role, 'nexora.membership_status', 'USAGE')
       OR has_type_privilege(api_role, 'nexora.tenant_role', 'USAGE')
       OR has_type_privilege(api_role, 'nexora.tenant_permission', 'USAGE') THEN
      RAISE EXCEPTION 'Data API role % must not use M2 enum types', api_role;
    END IF;
  END LOOP;

  IF has_type_privilege('public', 'nexora.membership_status', 'USAGE')
     OR has_type_privilege('public', 'nexora.tenant_role', 'USAGE')
     OR has_type_privilege('public', 'nexora.tenant_permission', 'USAGE') THEN
    RAISE EXCEPTION 'PUBLIC must not use M2 enum types';
  END IF;

  IF has_table_privilege('nexora_runtime', 'nexora.memberships', 'DELETE')
     OR has_table_privilege('nexora_runtime', 'nexora.organizations', 'DELETE')
     OR has_table_privilege('nexora_runtime', 'nexora.profiles', 'DELETE') THEN
    RAISE EXCEPTION 'runtime hard-delete privileges are forbidden for M2 lifecycle data';
  END IF;

  IF has_function_privilege('nexora_runtime', 'nexora.sync_membership_authorization()', 'EXECUTE') THEN
    RAISE EXCEPTION 'runtime must not invoke membership authorization synchronization directly';
  END IF;
END
$$;

DO $$
BEGIN
  IF (SELECT count(*) FROM nexora.tenant_role_permissions) <> 51 THEN
    RAISE EXCEPTION 'frozen tenant permission matrix must contain 51 grants';
  END IF;

  IF EXISTS (
    SELECT expected.tenant_role, expected.permission_count
    FROM (VALUES
      ('OWNER'::nexora.tenant_role, 13),
      ('ADMIN'::nexora.tenant_role, 13),
      ('EDITOR'::nexora.tenant_role, 10),
      ('CONTENT_CREATOR'::nexora.tenant_role, 6),
      ('REVIEWER'::nexora.tenant_role, 5),
      ('USER'::nexora.tenant_role, 4)
    ) AS expected(tenant_role, permission_count)
    LEFT JOIN (
      SELECT tenant_role, count(*)::integer AS permission_count
      FROM nexora.tenant_role_permissions
      GROUP BY tenant_role
    ) AS observed USING (tenant_role)
    WHERE observed.permission_count IS DISTINCT FROM expected.permission_count
  ) THEN
    RAISE EXCEPTION 'frozen role permission counts differ from M2-C01';
  END IF;
END
$$;

-- Alpha bootstrap: organization and first ACTIVE OWNER are one deferred unit.
BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000001', true);
INSERT INTO nexora.organizations (id, slug, name, owner_membership_id)
VALUES (
  '10000000-0000-4000-8000-000000000001',
  'alpha-university',
  'Alpha University',
  '30000000-0000-4000-8000-000000000001'
);
INSERT INTO nexora.memberships (id, organization_id, subject_id, status, tenant_role)
VALUES (
  '30000000-0000-4000-8000-000000000001',
  '10000000-0000-4000-8000-000000000001',
  '20000000-0000-4000-8000-000000000001',
  'ACTIVE',
  'OWNER'
);
COMMIT;

-- Beta bootstrap uses an unrelated subject and owner membership.
BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000003', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000002', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000004', true);
INSERT INTO nexora.organizations (id, slug, name, owner_membership_id)
VALUES (
  '10000000-0000-4000-8000-000000000002',
  'beta-institute',
  'Beta Institute',
  '30000000-0000-4000-8000-000000000004'
);
INSERT INTO nexora.memberships (id, organization_id, subject_id, status, tenant_role)
VALUES (
  '30000000-0000-4000-8000-000000000004',
  '10000000-0000-4000-8000-000000000002',
  '20000000-0000-4000-8000-000000000003',
  'ACTIVE',
  'OWNER'
);
COMMIT;

-- A subject with no membership must not reopen the bootstrap policy against a
-- committed tenant, even when both real organization and designated owner IDs
-- are supplied as transaction-local context.
BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000005', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000001', true);
DO $$
BEGIN
  IF (SELECT count(*) FROM nexora.organizations) <> 0
     OR (SELECT count(*) FROM nexora.memberships) <> 0 THEN
    RAISE EXCEPTION 'known committed organization/owner IDs must not authorize a non-member';
  END IF;
END
$$;
COMMIT;

-- Alpha owner assigns the exact frozen roles used by isolation/denial checks.
BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000001', true);
INSERT INTO nexora.memberships (id, organization_id, subject_id, status, tenant_role)
VALUES
  ('30000000-0000-4000-8000-000000000003', '10000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000002', 'ACTIVE', 'EDITOR'),
  ('30000000-0000-4000-8000-000000000005', '10000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000004', 'ACTIVE', 'ADMIN'),
  ('30000000-0000-4000-8000-000000000006', '10000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000006', 'ACTIVE', 'OWNER'),
  ('30000000-0000-4000-8000-000000000007', '10000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000007', 'ACTIVE', 'ADMIN');
COMMIT;

-- Beta owner gives Alice a USER membership, creating the multi-tenant subject.
BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000003', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000002', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000004', true);
INSERT INTO nexora.memberships (id, organization_id, subject_id, status, tenant_role)
VALUES (
  '30000000-0000-4000-8000-000000000002',
  '10000000-0000-4000-8000-000000000002',
  '20000000-0000-4000-8000-000000000001',
  'ACTIVE',
  'USER'
);
COMMIT;

-- Profiles are subject-scoped lifecycle data and remain authorization-neutral.
BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000001', true);
INSERT INTO nexora.profiles (subject_id, display_name, locale, reduced_motion, high_contrast)
VALUES ('20000000-0000-4000-8000-000000000001', 'Alice', 'en-US', false, false);
UPDATE nexora.profiles SET reduced_motion = true
WHERE subject_id = '20000000-0000-4000-8000-000000000001';
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM nexora.profiles
    WHERE subject_id = '20000000-0000-4000-8000-000000000001'
      AND version = 2
      AND reduced_motion
  ) THEN
    RAISE EXCEPTION 'profile update must increment lifecycle version';
  END IF;
END
$$;
COMMIT;

-- Missing context is default-deny for every protected relation.
BEGIN;
SET LOCAL ROLE nexora_runtime;
DO $$
BEGIN
  IF (SELECT count(*) FROM nexora.organizations) <> 0
     OR (SELECT count(*) FROM nexora.memberships) <> 0
     OR (SELECT count(*) FROM nexora.profiles) <> 0 THEN
    RAISE EXCEPTION 'missing transaction context must deny protected rows';
  END IF;
END
$$;
COMMIT;

-- Subject-only resolution sees exactly Alice's two ACTIVE memberships, but no
-- organization row before a full tenant context is selected.
BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000001', true);
DO $$
BEGIN
  IF (SELECT count(*) FROM nexora.memberships) <> 2 THEN
    RAISE EXCEPTION 'subject-only resolution must see two active Alice memberships';
  END IF;
  IF (SELECT count(*) FROM nexora.organizations) <> 0 THEN
    RAISE EXCEPTION 'subject-only resolution must not expose an organization row';
  END IF;
END
$$;
COMMIT;

-- Full Alpha OWNER context may enumerate Alpha memberships, but Beta remains
-- invisible despite Alice belonging to both tenants.
BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000001', true);
DO $$
BEGIN
  IF (SELECT count(*) FROM nexora.organizations) <> 1
     OR NOT EXISTS (
       SELECT 1 FROM nexora.organizations
       WHERE id = '10000000-0000-4000-8000-000000000001'
     ) THEN
    RAISE EXCEPTION 'Alpha context must expose Alpha only';
  END IF;
  IF EXISTS (
    SELECT 1 FROM nexora.organizations
    WHERE id = '10000000-0000-4000-8000-000000000002'
  ) THEN
    RAISE EXCEPTION 'Alpha context leaked Beta';
  END IF;
  IF (SELECT count(*) FROM nexora.memberships) <> 5 THEN
    RAISE EXCEPTION 'manager context must enumerate only Alpha memberships';
  END IF;
END
$$;
COMMIT;

-- An EDITOR cannot assign a role because both management permissions are
-- absent. The trigger rejects the write even though its tenant target matches.
BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000002', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000003', true);
DO $$
BEGIN
  IF (SELECT count(*) FROM nexora.memberships) <> 1 THEN
    RAISE EXCEPTION 'non-manager context must expose only the actor membership';
  END IF;
  BEGIN
    INSERT INTO nexora.memberships (id, organization_id, subject_id, status, tenant_role)
    VALUES (
      '30000000-0000-4000-8000-000000000008',
      '10000000-0000-4000-8000-000000000001',
      '20000000-0000-4000-8000-000000000008',
      'INVITED',
      'USER'
    );
    RAISE EXCEPTION 'EDITOR role assignment unexpectedly succeeded';
  EXCEPTION WHEN insufficient_privilege THEN
    NULL;
  END;
END
$$;
COMMIT;

-- ADMIN has both management permissions but still cannot assign OWNER.
BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000007', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000007', true);
DO $$
DECLARE
  updated_count bigint;
BEGIN
  BEGIN
    INSERT INTO nexora.memberships (id, organization_id, subject_id, status, tenant_role)
    VALUES (
      '30000000-0000-4000-8000-000000000009',
      '10000000-0000-4000-8000-000000000001',
      '20000000-0000-4000-8000-000000000009',
      'INVITED',
      'OWNER'
    );
    RAISE EXCEPTION 'ADMIN OWNER assignment unexpectedly succeeded';
  EXCEPTION WHEN insufficient_privilege THEN
    NULL;
  END;
END
$$;
COMMIT;

-- An ACTIVE manager with both required permissions may enumerate only Alpha.
-- The private authorization projection itself remains actor-only and cannot
-- be altered directly by runtime SQL.
BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000007', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000007', true);
DO $$
DECLARE
  updated_count bigint;
BEGIN
  IF (SELECT count(*) FROM nexora.memberships) <> 5
     OR NOT EXISTS (
       SELECT 1 FROM nexora.memberships
       WHERE id = '30000000-0000-4000-8000-000000000003'
     )
     OR EXISTS (
       SELECT 1 FROM nexora.memberships
       WHERE organization_id = '10000000-0000-4000-8000-000000000002'
     ) THEN
    RAISE EXCEPTION 'manager enumeration must include only selected-tenant memberships';
  END IF;

  IF (SELECT count(*) FROM nexora.membership_authorizations) <> 1 THEN
    RAISE EXCEPTION 'runtime must not enumerate membership authorization projections';
  END IF;

  UPDATE nexora.membership_authorizations
  SET tenant_role = 'OWNER'
  WHERE membership_id = '30000000-0000-4000-8000-000000000007';
  GET DIAGNOSTICS updated_count = ROW_COUNT;
  IF updated_count <> 0 THEN
    RAISE EXCEPTION 'runtime directly mutated the private authorization projection';
  END IF;
END
$$;
COMMIT;

-- ADMIN may inspect and change exactly Bob's current row when both target ID
-- and expected version are transaction-local. The same policy prevents a stale
-- expected version and any cross-tenant target before the write is attempted.
BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000007', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000007', true);
SELECT set_config('nexora.target_membership_id', '30000000-0000-4000-8000-000000000003', true);
SELECT set_config('nexora.target_membership_version', '1', true);
DO $$
DECLARE
  updated_count bigint;
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM nexora.memberships
    WHERE id = '30000000-0000-4000-8000-000000000003'
      AND organization_id = '10000000-0000-4000-8000-000000000001'
      AND version = 1
      AND tenant_role = 'EDITOR'
  ) THEN
    RAISE EXCEPTION 'manager must inspect the exact expected target membership';
  END IF;

  PERFORM set_config('nexora.target_membership_mutation', 'true', true);
  UPDATE nexora.memberships
  SET tenant_role = 'REVIEWER'
  WHERE id = '30000000-0000-4000-8000-000000000003';
  GET DIAGNOSTICS updated_count = ROW_COUNT;
  IF updated_count <> 1 THEN
    RAISE EXCEPTION 'manager exact target update must affect one row, observed %', updated_count;
  END IF;

  PERFORM set_config('nexora.target_membership_mutation', '', true);

  IF NOT EXISTS (
    SELECT 1 FROM nexora.memberships
    WHERE id = '30000000-0000-4000-8000-000000000003'
      AND version = 2
  ) THEN
    RAISE EXCEPTION 'manager enumeration must show the current target version';
  END IF;

  UPDATE nexora.memberships
  SET tenant_role = 'EDITOR'
  WHERE id = '30000000-0000-4000-8000-000000000003';
  GET DIAGNOSTICS updated_count = ROW_COUNT;
  IF updated_count <> 0 THEN
    RAISE EXCEPTION 'stale target version unexpectedly updated % row(s)', updated_count;
  END IF;

  PERFORM set_config('nexora.target_membership_id', '30000000-0000-4000-8000-000000000004', true);
  PERFORM set_config('nexora.target_membership_version', '1', true);
  IF EXISTS (
    SELECT 1 FROM nexora.memberships
    WHERE id = '30000000-0000-4000-8000-000000000004'
  ) THEN
    RAISE EXCEPTION 'Alpha management context leaked a Beta target';
  END IF;
END
$$;
COMMIT;

-- Alice changes Dave from ADMIN to EDITOR. A caller holding Dave's previous
-- ADMIN/version context is then denied target visibility and mutation because
-- the private projection re-reads the current actor role under forced RLS.
BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.target_membership_id', '30000000-0000-4000-8000-000000000005', true);
SELECT set_config('nexora.target_membership_version', '1', true);
SELECT set_config('nexora.target_membership_mutation', 'true', true);
UPDATE nexora.memberships
SET tenant_role = 'EDITOR'
WHERE id = '30000000-0000-4000-8000-000000000005';
COMMIT;

BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000004', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000005', true);
SELECT set_config('nexora.target_membership_id', '30000000-0000-4000-8000-000000000003', true);
SELECT set_config('nexora.target_membership_version', '2', true);
DO $$
DECLARE
  updated_count bigint;
BEGIN
  IF (SELECT count(*) FROM nexora.memberships) <> 1 THEN
    RAISE EXCEPTION 'stale manager authority unexpectedly enumerated memberships';
  END IF;
  IF EXISTS (
    SELECT 1 FROM nexora.memberships
    WHERE id = '30000000-0000-4000-8000-000000000003'
  ) THEN
    RAISE EXCEPTION 'stale actor role/version unexpectedly read a target membership';
  END IF;

  UPDATE nexora.memberships
  SET tenant_role = 'USER'
  WHERE id = '30000000-0000-4000-8000-000000000003';
  GET DIAGNOSTICS updated_count = ROW_COUNT;
  IF updated_count <> 0 THEN
    RAISE EXCEPTION 'stale actor role/version unexpectedly updated % row(s)', updated_count;
  END IF;
END
$$;
COMMIT;

-- Alice removes Dave through the same runtime-only target contract. A removed
-- actor cannot revive a target operation even if old full-context and target
-- settings are supplied on a pooled connection.
BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.target_membership_id', '30000000-0000-4000-8000-000000000005', true);
SELECT set_config('nexora.target_membership_version', '2', true);
SELECT set_config('nexora.target_membership_mutation', 'true', true);
UPDATE nexora.memberships
SET status = 'REMOVED'
WHERE id = '30000000-0000-4000-8000-000000000005';
COMMIT;

BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000004', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000005', true);
SELECT set_config('nexora.target_membership_id', '30000000-0000-4000-8000-000000000003', true);
SELECT set_config('nexora.target_membership_version', '2', true);
DO $$
DECLARE
  updated_count bigint;
BEGIN
  IF EXISTS (
    SELECT 1 FROM nexora.memberships
    WHERE id = '30000000-0000-4000-8000-000000000003'
  ) THEN
    RAISE EXCEPTION 'REMOVED actor unexpectedly read a target membership';
  END IF;

  UPDATE nexora.memberships
  SET tenant_role = 'USER'
  WHERE id = '30000000-0000-4000-8000-000000000003';
  GET DIAGNOSTICS updated_count = ROW_COUNT;
  IF updated_count <> 0 THEN
    RAISE EXCEPTION 'REMOVED actor unexpectedly updated % row(s)', updated_count;
  END IF;
END
$$;
COMMIT;

BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000004', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000005', true);
DO $$
BEGIN
  IF (SELECT count(*) FROM nexora.memberships) <> 0
     OR (SELECT count(*) FROM nexora.organizations) <> 0 THEN
    RAISE EXCEPTION 'REMOVED membership context must deny the next request';
  END IF;
END
$$;
COMMIT;

-- The REMOVED subject also cannot substitute the committed tenant's real
-- designated owner membership id to reopen pending-bootstrap visibility.
BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000004', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000001', true);
DO $$
BEGIN
  IF (SELECT count(*) FROM nexora.organizations) <> 0
     OR (SELECT count(*) FROM nexora.memberships) <> 0 THEN
    RAISE EXCEPTION 'REMOVED subject with real committed organization/owner IDs must be denied';
  END IF;
END
$$;
COMMIT;

-- Beta's only owner cannot be demoted, suspended, or removed. Each attempted
-- transition violates the deferred composite ACTIVE OWNER reference.
BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000003', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000002', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000004', true);
SELECT set_config('nexora.target_membership_id', '30000000-0000-4000-8000-000000000004', true);
SELECT set_config('nexora.target_membership_version', '1', true);
SELECT set_config('nexora.target_membership_mutation', 'true', true);
DO $$
DECLARE
  blocked_status nexora.membership_status;
BEGIN
  FOREACH blocked_status IN ARRAY ARRAY[
    'SUSPENDED'::nexora.membership_status,
    'REMOVED'::nexora.membership_status
  ]
  LOOP
    BEGIN
      UPDATE nexora.memberships
      SET status = blocked_status
      WHERE id = '30000000-0000-4000-8000-000000000004';
      SET CONSTRAINTS ALL IMMEDIATE;
      RAISE EXCEPTION 'last-owner % unexpectedly succeeded', blocked_status;
    EXCEPTION WHEN foreign_key_violation THEN
      SET CONSTRAINTS ALL DEFERRED;
    END;
  END LOOP;

  BEGIN
    UPDATE nexora.memberships
    SET tenant_role = 'ADMIN'
    WHERE id = '30000000-0000-4000-8000-000000000004';
    SET CONSTRAINTS ALL IMMEDIATE;
    RAISE EXCEPTION 'last-owner demotion unexpectedly succeeded';
  EXCEPTION WHEN foreign_key_violation THEN
    SET CONSTRAINTS ALL DEFERRED;
  END;
END
$$;
COMMIT;

-- Alpha has a second ACTIVE OWNER, so the designated owner can be transferred
-- and the former owner demoted atomically without ever leaving the tenant empty.
BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.target_membership_id', '30000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.target_membership_version', '1', true);
SELECT set_config('nexora.target_membership_mutation', 'true', true);
UPDATE nexora.organizations
SET owner_membership_id = '30000000-0000-4000-8000-000000000006'
WHERE id = '10000000-0000-4000-8000-000000000001';
UPDATE nexora.memberships
SET tenant_role = 'ADMIN'
WHERE id = '30000000-0000-4000-8000-000000000001';
SET CONSTRAINTS ALL IMMEDIATE;
COMMIT;

-- SET LOCAL values must not survive commit on the pooled session.
BEGIN;
SET LOCAL ROLE nexora_runtime;
DO $$
BEGIN
  IF NULLIF(current_setting('nexora.subject_id', true), '') IS NOT NULL
     OR NULLIF(current_setting('nexora.organization_id', true), '') IS NOT NULL
     OR NULLIF(current_setting('nexora.membership_id', true), '') IS NOT NULL
     OR NULLIF(current_setting('nexora.target_membership_id', true), '') IS NOT NULL
     OR NULLIF(current_setting('nexora.target_membership_version', true), '') IS NOT NULL
     OR NULLIF(current_setting('nexora.target_membership_mutation', true), '') IS NOT NULL THEN
    RAISE EXCEPTION 'transaction-local context leaked across commit';
  END IF;
  IF (SELECT count(*) FROM nexora.organizations) <> 0
     OR (SELECT count(*) FROM nexora.memberships) <> 0
     OR (SELECT count(*) FROM nexora.membership_authorizations) <> 0
     OR (SELECT count(*) FROM nexora.profiles) <> 0 THEN
    RAISE EXCEPTION 'pooled session without new context must remain denied';
  END IF;
END
$$;
COMMIT;

SELECT
  'M2-DB01 schema/auth verification passed' AS result,
  current_database() AS database_name,
  current_setting('server_version') AS postgresql_version;
