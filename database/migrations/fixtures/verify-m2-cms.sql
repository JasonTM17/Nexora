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
INSERT INTO nexora.theme_versions (id, organization_id, theme_id, version, state, token_digest, token_manifest, actor_id)
VALUES ('60000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000001', '50000000-0000-4000-8000-000000000001', 2, 'PUBLISHED', 'sha256:abababababababababababababababababababababababababababababababab', '{"color":"safe-v2"}'::jsonb, '20000000-0000-4000-8000-000000000001');
INSERT INTO nexora.memberships (id, organization_id, subject_id, status, tenant_role)
VALUES
  ('30000000-0000-4000-8000-000000000008', '10000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000008', 'ACTIVE', 'CONTENT_CREATOR'),
  ('30000000-0000-4000-8000-000000000009', '10000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000009', 'ACTIVE', 'EDITOR');
INSERT INTO nexora.pages (id, organization_id, site_id, slug, title, schema_version, content_digest, theme_version_id, seo_title, seo_description, seo_locale, canonical_path, state, published_version_id)
VALUES ('70000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000001', 'welcome', 'Welcome', '1.0.0', 'sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb', '60000000-0000-4000-8000-000000000001', 'Welcome', 'Welcome to Alpha.', 'en-US', '/welcome', 'PUBLISHED', '80000000-0000-4000-8000-000000000001');
INSERT INTO nexora.page_versions (id, organization_id, site_id, page_id, source_draft_version, publication_operation, schema_version, content_digest, theme_version_id, seo_title, seo_description, seo_locale, canonical_path, seo_snapshot_digest, actor_id, trace_id)
VALUES ('80000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000001', '70000000-0000-4000-8000-000000000001', 1, 'PUBLISH', '1.0.0', 'sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb', '60000000-0000-4000-8000-000000000001', 'Welcome', 'Welcome to Alpha.', 'en-US', '/welcome', 'sha256:cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc', '20000000-0000-4000-8000-000000000001', 'cms-fixture-1');
INSERT INTO nexora.page_publications (id, organization_id, site_id, page_id, source_draft_version, published_version_id, publication_operation, actor_id, trace_id, idempotency_key_digest, request_fingerprint)
VALUES ('90000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000001', '70000000-0000-4000-8000-000000000001', 1, '80000000-0000-4000-8000-000000000001', 'PUBLISH', '20000000-0000-4000-8000-000000000001', 'cms-fixture-1', 'sha256:dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd', 'sha256:eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee');
INSERT INTO nexora.workflow_reviews (id, organization_id, page_id, candidate_draft_version, action, from_state, to_state, actor_id, trace_id)
VALUES ('a0000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', '70000000-0000-4000-8000-000000000001', 1, 'SUBMIT_FOR_REVIEW', 'DRAFT', 'IN_REVIEW', '20000000-0000-4000-8000-000000000001', 'cms-fixture-1');
INSERT INTO nexora.cms_audit_events (id, organization_id, site_id, page_id, version_id, operation, result, actor_id, trace_id, idempotency_key_digest)
VALUES ('b0000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000001', '70000000-0000-4000-8000-000000000001', '80000000-0000-4000-8000-000000000001', 'PUBLISH', 'ACCEPTED', '20000000-0000-4000-8000-000000000001', 'cms-fixture-1', 'sha256:dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd');
INSERT INTO nexora.cms_audit_events (id, organization_id, site_id, page_id, operation, result, actor_id, trace_id)
VALUES
  ('b0000000-0000-4000-8000-000000000004', '10000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000001', '70000000-0000-4000-8000-000000000001', 'PAGE_CREATE', 'ACCEPTED', '20000000-0000-4000-8000-000000000001', 'cms-page-create'),
  ('b0000000-0000-4000-8000-000000000005', '10000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000001', '70000000-0000-4000-8000-000000000001', 'PAGE_UPDATE', 'ACCEPTED', '20000000-0000-4000-8000-000000000001', 'cms-page-update');

INSERT INTO nexora.pages (id, organization_id, site_id, slug, title, schema_version, content_digest, theme_version_id, seo_title, seo_description, seo_locale, canonical_path)
VALUES ('70000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000001', 'about', 'About', '1.0.0', 'sha256:ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff', '60000000-0000-4000-8000-000000000001', 'About', 'About Alpha.', 'en-US', '/about');
INSERT INTO nexora.page_versions (id, organization_id, site_id, page_id, source_draft_version, publication_operation, schema_version, content_digest, theme_version_id, seo_title, seo_description, seo_locale, canonical_path, seo_snapshot_digest, actor_id, trace_id)
VALUES ('80000000-0000-4000-8000-000000000004', '10000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000001', '70000000-0000-4000-8000-000000000002', 1, 'PUBLISH', '1.0.0', 'sha256:ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff', '60000000-0000-4000-8000-000000000001', 'About', 'About Alpha.', 'en-US', '/about', 'sha256:dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd', '20000000-0000-4000-8000-000000000001', 'cms-fixture-about-1');
INSERT INTO nexora.page_versions (id, organization_id, site_id, page_id, source_draft_version, publication_operation, schema_version, content_digest, theme_version_id, seo_title, seo_description, seo_locale, canonical_path, seo_snapshot_digest, actor_id, trace_id)
VALUES ('80000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000001', '70000000-0000-4000-8000-000000000001', 2, 'PUBLISH', '1.0.0', 'sha256:ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff', '60000000-0000-4000-8000-000000000001', 'Welcome revision two', 'Welcome revision two.', 'en-US', '/welcome', 'sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', '20000000-0000-4000-8000-000000000001', 'cms-fixture-2');
INSERT INTO nexora.page_versions (id, organization_id, site_id, page_id, source_draft_version, publication_operation, schema_version, content_digest, theme_version_id, seo_title, seo_description, seo_locale, canonical_path, seo_snapshot_digest, actor_id, trace_id)
VALUES ('80000000-0000-4000-8000-000000000003', '10000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000001', '70000000-0000-4000-8000-000000000001', 3, 'PUBLISH', '1.0.0', 'sha256:1111111111111111111111111111111111111111111111111111111111111111', '60000000-0000-4000-8000-000000000001', 'Welcome revision three', 'Welcome revision three.', 'en-US', '/welcome', 'sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb', '20000000-0000-4000-8000-000000000001', 'cms-fixture-3');

DO $$
BEGIN
  IF (SELECT count(*) FROM nexora.pages WHERE slug = 'welcome') <> 1
     OR (SELECT count(*) FROM nexora.page_versions) <> 4
     OR (SELECT count(*) FROM nexora.page_publications) <> 1 THEN
    RAISE EXCEPTION 'same-tenant CMS rows were not visible to authorized owner';
  END IF;
END $$;

DO $$
BEGIN
  BEGIN
    UPDATE nexora.pages
    SET published_version_id = '80000000-0000-4000-8000-000000000001',
        draft_version = draft_version + 1
    WHERE id = '70000000-0000-4000-8000-000000000002';
    RAISE EXCEPTION 'DRAFT page published-version pointer mutation unexpectedly succeeded';
  EXCEPTION WHEN insufficient_privilege THEN NULL;
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

-- CONTENT_CREATOR may make a state-stable DRAFT edit, but may not change any
-- PUBLISHED payload, SEO, theme, pointer, or lifecycle state through page.update.
BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000008', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000008', true);
DO $$
BEGIN
  UPDATE nexora.pages SET title = 'PUBLISHED title mutation'
  WHERE id = '70000000-0000-4000-8000-000000000001';
  IF FOUND THEN
    RAISE EXCEPTION 'CONTENT_CREATOR PUBLISHED title mutation unexpectedly succeeded';
  END IF;

  UPDATE nexora.pages SET seo_title = 'PUBLISHED SEO mutation'
  WHERE id = '70000000-0000-4000-8000-000000000001';
  IF FOUND THEN
    RAISE EXCEPTION 'CONTENT_CREATOR PUBLISHED SEO mutation unexpectedly succeeded';
  END IF;

  UPDATE nexora.pages SET theme_version_id = '60000000-0000-4000-8000-000000000002'
  WHERE id = '70000000-0000-4000-8000-000000000001';
  IF FOUND THEN
    RAISE EXCEPTION 'CONTENT_CREATOR PUBLISHED theme mutation unexpectedly succeeded';
  END IF;

  UPDATE nexora.pages SET published_version_id = '80000000-0000-4000-8000-000000000002'
  WHERE id = '70000000-0000-4000-8000-000000000001';
  IF FOUND THEN
    RAISE EXCEPTION 'CONTENT_CREATOR PUBLISHED pointer mutation unexpectedly succeeded';
  END IF;

  UPDATE nexora.pages SET state = 'ARCHIVED'
  WHERE id = '70000000-0000-4000-8000-000000000001';
  IF FOUND THEN
    RAISE EXCEPTION 'CONTENT_CREATOR archive unexpectedly succeeded';
  END IF;

  UPDATE nexora.pages
  SET title = 'About draft revised', draft_version = draft_version + 1
  WHERE id = '70000000-0000-4000-8000-000000000002';
  IF NOT FOUND THEN
    RAISE EXCEPTION 'CONTENT_CREATOR DRAFT update unexpectedly failed';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM nexora.pages
    WHERE id = '70000000-0000-4000-8000-000000000002'
      AND title = 'About draft revised'
      AND state = 'DRAFT'
      AND draft_version = 2
  ) THEN
    RAISE EXCEPTION 'CONTENT_CREATOR DRAFT update did not preserve exact draft semantics';
  END IF;

  BEGIN
    UPDATE nexora.pages
    SET published_version_id = '80000000-0000-4000-8000-000000000004',
        draft_version = draft_version + 1
    WHERE id = '70000000-0000-4000-8000-000000000002';
    RAISE EXCEPTION 'CONTENT_CREATOR DRAFT published pointer mutation unexpectedly succeeded';
  EXCEPTION WHEN insufficient_privilege THEN NULL;
  END;
END $$;
COMMIT;

-- REVIEWER has page.publish but not page.update: direct content/SEO edits are
-- denied, while the frozen PUBLISHED -> ARCHIVED transition is allowed.
BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000002', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000003', true);
DO $$
DECLARE
  prior_version bigint;
BEGIN
  SELECT version INTO prior_version FROM nexora.pages WHERE id = '70000000-0000-4000-8000-000000000001';
  BEGIN
    UPDATE nexora.pages SET seo_title = 'Reviewer edit'
    WHERE id = '70000000-0000-4000-8000-000000000001';
    RAISE EXCEPTION 'REVIEWER direct SEO edit unexpectedly succeeded';
  EXCEPTION WHEN insufficient_privilege THEN NULL;
  END;

  UPDATE nexora.pages SET state = 'ARCHIVED'
  WHERE id = '70000000-0000-4000-8000-000000000001';
  IF NOT EXISTS (
    SELECT 1 FROM nexora.pages
    WHERE id = '70000000-0000-4000-8000-000000000001'
      AND state = 'ARCHIVED'
      AND title = 'Welcome'
      AND version = prior_version + 1
  ) THEN
    RAISE EXCEPTION 'REVIEWER PUBLISHED to ARCHIVED workflow transition failed or changed payload';
  END IF;

  BEGIN
    UPDATE nexora.pages SET state = 'DRAFT'
    WHERE id = '70000000-0000-4000-8000-000000000001';
    RAISE EXCEPTION 'ARCHIVED to DRAFT workflow transition unexpectedly succeeded';
  EXCEPTION WHEN insufficient_privilege THEN NULL;
  END;
END $$;
COMMIT;

-- REVIEWER retains publication audit but cannot forge PAGE_CREATE/PAGE_UPDATE
-- audit rows, a different actor, or an arbitrary operation.
BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000002', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000001', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000003', true);
INSERT INTO nexora.cms_audit_events (id, organization_id, site_id, page_id, version_id, operation, result, actor_id, trace_id)
VALUES ('b0000000-0000-4000-8000-000000000006', '10000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000001', '70000000-0000-4000-8000-000000000001', '80000000-0000-4000-8000-000000000001', 'PUBLISH', 'ACCEPTED', '20000000-0000-4000-8000-000000000002', 'cms-reviewer-publish');
DO $$
BEGIN
  BEGIN
    INSERT INTO nexora.cms_audit_events (id, organization_id, site_id, page_id, operation, result, actor_id, trace_id)
    VALUES ('b0000000-0000-4000-8000-000000000007', '10000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000001', '70000000-0000-4000-8000-000000000001', 'PAGE_CREATE', 'ACCEPTED', '20000000-0000-4000-8000-000000000002', 'cms-reviewer-create');
    RAISE EXCEPTION 'REVIEWER PAGE_CREATE audit unexpectedly succeeded';
  EXCEPTION WHEN insufficient_privilege THEN NULL;
  END;
  BEGIN
    INSERT INTO nexora.cms_audit_events (id, organization_id, site_id, page_id, operation, result, actor_id, trace_id)
    VALUES ('b0000000-0000-4000-8000-000000000008', '10000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000001', '70000000-0000-4000-8000-000000000001', 'PAGE_UPDATE', 'ACCEPTED', '20000000-0000-4000-8000-000000000002', 'cms-reviewer-update');
    RAISE EXCEPTION 'REVIEWER PAGE_UPDATE audit unexpectedly succeeded';
  EXCEPTION WHEN insufficient_privilege THEN NULL;
  END;
  BEGIN
    INSERT INTO nexora.cms_audit_events (id, organization_id, site_id, page_id, operation, result, actor_id, trace_id)
    VALUES ('b0000000-0000-4000-8000-000000000009', '10000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000001', '70000000-0000-4000-8000-000000000001', 'PUBLISH', 'ACCEPTED', '20000000-0000-4000-8000-000000000001', 'cms-forged-actor');
    RAISE EXCEPTION 'forged CMS audit actor unexpectedly succeeded';
  EXCEPTION WHEN insufficient_privilege THEN NULL;
  END;
  BEGIN
    INSERT INTO nexora.cms_audit_events (id, organization_id, site_id, page_id, operation, result, actor_id, trace_id)
    VALUES ('b0000000-0000-4000-8000-000000000010', '10000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000001', '70000000-0000-4000-8000-000000000001', 'UNSAFE', 'ACCEPTED', '20000000-0000-4000-8000-000000000002', 'cms-unsafe-operation');
    RAISE EXCEPTION 'arbitrary CMS audit operation unexpectedly succeeded';
  EXCEPTION WHEN insufficient_privilege THEN NULL;
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

-- An ACTIVE actor in another tenant cannot append an Alpha audit row even with
-- Alpha IDs and a page.publish-capable role in its own selected tenant.
BEGIN;
SET LOCAL ROLE nexora_runtime;
SELECT set_config('nexora.subject_id', '20000000-0000-4000-8000-000000000003', true);
SELECT set_config('nexora.organization_id', '10000000-0000-4000-8000-000000000002', true);
SELECT set_config('nexora.membership_id', '30000000-0000-4000-8000-000000000004', true);
DO $$
BEGIN
  BEGIN
    INSERT INTO nexora.cms_audit_events (id, organization_id, site_id, page_id, operation, result, actor_id, trace_id)
    VALUES ('b0000000-0000-4000-8000-000000000011', '10000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000001', '70000000-0000-4000-8000-000000000001', 'PUBLISH', 'ACCEPTED', '20000000-0000-4000-8000-000000000003', 'cms-cross-tenant');
    RAISE EXCEPTION 'cross-tenant CMS audit insert unexpectedly succeeded';
  EXCEPTION WHEN insufficient_privilege THEN NULL;
  END;
END $$;
COMMIT;

BEGIN;
SET LOCAL ROLE nexora_runtime;
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM nexora.pages) OR EXISTS (SELECT 1 FROM nexora.page_versions) THEN
    RAISE EXCEPTION 'missing tenant context must deny CMS rows';
  END IF;
  BEGIN
    INSERT INTO nexora.cms_audit_events (id, organization_id, operation, result, actor_id, trace_id)
    VALUES ('b0000000-0000-4000-8000-000000000012', '10000000-0000-4000-8000-000000000001', 'PUBLISH', 'DENIED', '20000000-0000-4000-8000-000000000001', 'cms-missing-context');
    RAISE EXCEPTION 'missing tenant context unexpectedly appended CMS audit';
  EXCEPTION WHEN insufficient_privilege THEN NULL;
  END;
END $$;
COMMIT;

SELECT 'M2-DB02 CMS verification passed' AS result, current_setting('server_version') AS postgresql_version;
