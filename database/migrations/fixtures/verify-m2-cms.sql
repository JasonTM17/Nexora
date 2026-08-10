\set ON_ERROR_STOP on

-- Runs after the existing M2 authority fixture in the same disposable
-- PostgreSQL 17.5 database. UUIDs below are synthetic fixture values only.

DO $$
BEGIN
  IF (SELECT count(*) FROM pg_class relation
      JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
      WHERE namespace.nspname = 'nexora'
        AND relation.relname IN ('sites','themes','theme_versions','media_assets','pages','page_versions','page_publications','workflow_reviews','cms_audit_events')
        AND relation.relrowsecurity AND relation.relforcerowsecurity) <> 9 THEN
    RAISE EXCEPTION 'every M2 CMS relation must enable and force RLS';
  END IF;
  IF EXISTS (
    SELECT 1 FROM pg_proc procedure
    JOIN pg_namespace namespace ON namespace.oid = procedure.pronamespace
    WHERE namespace.nspname = 'nexora' AND procedure.prosecdef
  ) THEN
    RAISE EXCEPTION 'CMS migrations must not create SECURITY DEFINER functions';
  END IF;
END $$;

BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000001', true);

INSERT INTO nexora.sites (id, organization_id, slug, canonical_host)
VALUES ('40000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'main', 'alpha.example.test');
INSERT INTO nexora.themes (id, organization_id, slug)
VALUES ('50000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'default');
INSERT INTO nexora.theme_versions (id, organization_id, theme_id, version, state, token_digest, token_manifest, actor_id)
VALUES ('60000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', '50000000-0000-4000-8000-000000000001', 1, 'PUBLISHED', 'sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', '{"color":"safe"}'::jsonb, '20000000-0000-4000-8000-000000000001');
INSERT INTO nexora.pages (id, organization_id, site_id, slug, title, schema_version, content_digest, theme_version_id, seo_title, seo_description, seo_locale, canonical_path)
VALUES ('70000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000001', 'welcome', 'Welcome', '1.0.0', 'sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb', '60000000-0000-4000-8000-000000000001', 'Welcome', 'Welcome to Alpha.', 'en-US', '/welcome');
INSERT INTO nexora.page_versions (id, organization_id, site_id, page_id, source_draft_version, publication_operation, schema_version, content_digest, theme_version_id, seo_title, seo_description, seo_locale, canonical_path, seo_snapshot_digest, actor_id, trace_id)
VALUES ('80000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000001', '70000000-0000-4000-8000-000000000001', 1, 'PUBLISH', '1.0.0', 'sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb', '60000000-0000-4000-8000-000000000001', 'Welcome', 'Welcome to Alpha.', 'en-US', '/welcome', 'sha256:cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc', '20000000-0000-4000-8000-000000000001', 'cms-fixture-1');
UPDATE nexora.pages SET state = 'PUBLISHED', published_version_id = '80000000-0000-4000-8000-000000000001' WHERE id = '70000000-0000-4000-8000-000000000001';
INSERT INTO nexora.page_publications (id, organization_id, site_id, page_id, source_draft_version, published_version_id, publication_operation, actor_id, trace_id, idempotency_key_digest, request_fingerprint)
VALUES ('90000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000001', '70000000-0000-4000-8000-000000000001', 1, '80000000-0000-4000-8000-000000000001', 'PUBLISH', '20000000-0000-4000-8000-000000000001', 'cms-fixture-1', 'sha256:dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd', 'sha256:eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee');
INSERT INTO nexora.workflow_reviews (id, organization_id, page_id, candidate_draft_version, action, from_state, to_state, actor_id, trace_id)
VALUES ('a0000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', '70000000-0000-4000-8000-000000000001', 1, 'SUBMIT_FOR_REVIEW', 'DRAFT', 'IN_REVIEW', '20000000-0000-4000-8000-000000000001', 'cms-fixture-1');
INSERT INTO nexora.cms_audit_events (id, organization_id, site_id, page_id, version_id, operation, result, actor_id, trace_id, idempotency_key_digest)
VALUES ('b0000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000001', '70000000-0000-4000-8000-000000000001', '80000000-0000-4000-8000-000000000001', 'PUBLISH', 'ACCEPTED', '20000000-0000-4000-8000-000000000001', 'cms-fixture-1', 'sha256:dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd');

INSERT INTO nexora.pages (id, organization_id, site_id, slug, title, schema_version, content_digest, theme_version_id, seo_title, seo_description, seo_locale, canonical_path)
VALUES ('70000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000001', 'about', 'About', '1.0.0', 'sha256:ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff', '60000000-0000-4000-8000-000000000001', 'About', 'About Alpha.', 'en-US', '/about');
INSERT INTO nexora.page_versions (id, organization_id, site_id, page_id, source_draft_version, publication_operation, schema_version, content_digest, theme_version_id, seo_title, seo_description, seo_locale, canonical_path, seo_snapshot_digest, actor_id, trace_id)
VALUES ('80000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000001', '70000000-0000-4000-8000-000000000001', 2, 'PUBLISH', '1.0.0', 'sha256:ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff', '60000000-0000-4000-8000-000000000001', 'Welcome revision two', 'Welcome revision two.', 'en-US', '/welcome', 'sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', '20000000-0000-4000-8000-000000000001', 'cms-fixture-2');
INSERT INTO nexora.page_versions (id, organization_id, site_id, page_id, source_draft_version, publication_operation, schema_version, content_digest, theme_version_id, seo_title, seo_description, seo_locale, canonical_path, seo_snapshot_digest, actor_id, trace_id)
VALUES ('80000000-0000-4000-8000-000000000003', '10000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000001', '70000000-0000-4000-8000-000000000001', 3, 'PUBLISH', '1.0.0', 'sha256:1111111111111111111111111111111111111111111111111111111111111111', '60000000-0000-4000-8000-000000000001', 'Welcome revision three', 'Welcome revision three.', 'en-US', '/welcome', 'sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb', '20000000-0000-4000-8000-000000000001', 'cms-fixture-3');

DO $$
BEGIN
  IF (SELECT count(*) FROM nexora.pages WHERE slug = 'welcome') <> 1
     OR (SELECT count(*) FROM nexora.page_versions) <> 3
     OR (SELECT count(*) FROM nexora.page_publications) <> 1 THEN
    RAISE EXCEPTION 'same-tenant CMS rows were not visible to authorized owner';
  END IF;
END $$;

DO $$
BEGIN
  BEGIN
    UPDATE nexora.pages
    SET published_version_id = '80000000-0000-4000-8000-000000000001'
    WHERE id = '70000000-0000-4000-8000-000000000002';
    SET CONSTRAINTS ALL IMMEDIATE;
    RAISE EXCEPTION 'same-tenant cross-page published version unexpectedly succeeded';
  EXCEPTION WHEN foreign_key_violation THEN
    SET CONSTRAINTS ALL DEFERRED;
  END;

  BEGIN
    INSERT INTO nexora.page_publications (id, organization_id, site_id, page_id, source_draft_version, published_version_id, publication_operation, actor_id, trace_id, idempotency_key_digest, request_fingerprint)
    VALUES ('90000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000001', '70000000-0000-4000-8000-000000000002', 2, '80000000-0000-4000-8000-000000000002', 'PUBLISH', '20000000-0000-4000-8000-000000000001', 'cms-fixture-cross-page', 'sha256:1111111111111111111111111111111111111111111111111111111111111111', 'sha256:2222222222222222222222222222222222222222222222222222222222222222');
    RAISE EXCEPTION 'same-tenant cross-page publication receipt unexpectedly succeeded';
  EXCEPTION WHEN foreign_key_violation THEN NULL;
  END;

  BEGIN
    INSERT INTO nexora.page_publications (id, organization_id, site_id, page_id, source_draft_version, published_version_id, publication_operation, actor_id, trace_id, idempotency_key_digest, request_fingerprint)
    VALUES ('90000000-0000-4000-8000-000000000003', '10000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000001', '70000000-0000-4000-8000-000000000001', 3, '80000000-0000-4000-8000-000000000003', 'ROLLBACK', '20000000-0000-4000-8000-000000000001', 'cms-fixture-operation-mismatch', 'sha256:3333333333333333333333333333333333333333333333333333333333333333', 'sha256:4444444444444444444444444444444444444444444444444444444444444444');
    RAISE EXCEPTION 'same-page publication operation mismatch unexpectedly succeeded';
  EXCEPTION WHEN foreign_key_violation THEN NULL;
  END;
END $$;
COMMIT;

-- Use the disposable database's bootstrap role only to prove the trigger is
-- immutable even outside runtime grants; runtime itself has no UPDATE grant.
DO $$
BEGIN
  BEGIN
    UPDATE nexora.page_versions SET seo_title = 'Mutated' WHERE id = '80000000-0000-4000-8000-000000000001';
    RAISE EXCEPTION 'immutable page version unexpectedly updated';
  EXCEPTION WHEN object_not_in_prerequisite_state THEN NULL;
  END;
END $$;

BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000003', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000002', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000004', true);
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM nexora.pages WHERE id = '70000000-0000-4000-8000-000000000001')
     OR EXISTS (SELECT 1 FROM nexora.page_versions WHERE id = '80000000-0000-4000-8000-000000000001') THEN
    RAISE EXCEPTION 'cross-tenant CMS visibility escaped forced RLS';
  END IF;
END $$;
COMMIT;

BEGIN;
SET LOCAL ROLE nexora_runtime;
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM nexora.pages) OR EXISTS (SELECT 1 FROM nexora.page_versions) THEN
    RAISE EXCEPTION 'missing tenant context must deny CMS rows';
  END IF;
END $$;
COMMIT;

SELECT 'M2-DB02 CMS verification passed' AS result, current_setting('server_version') AS postgresql_version;
