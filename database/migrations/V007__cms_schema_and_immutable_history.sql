-- M2-DB02 CMS aggregate, immutable publication history, theme, workflow and
-- typed SEO storage. Runtime access is deliberately absent here; V008 adds
-- forced-RLS policies and only the narrow column grants required by later
-- backend workers.

BEGIN;
SET LOCAL ROLE nexora_migrator;

CREATE TYPE nexora.page_state AS ENUM (
  'DRAFT',
  'IN_REVIEW',
  'APPROVED',
  'PUBLISHED',
  'ARCHIVED'
);

CREATE TYPE nexora.theme_version_state AS ENUM (
  'DRAFT',
  'PUBLISHED',
  'ARCHIVED'
);

CREATE TYPE nexora.publication_operation AS ENUM (
  'PUBLISH',
  'ROLLBACK'
);

CREATE TYPE nexora.workflow_action AS ENUM (
  'SUBMIT_FOR_REVIEW',
  'APPROVE',
  'REJECT'
);

REVOKE USAGE ON TYPE
  nexora.page_state,
  nexora.theme_version_state,
  nexora.publication_operation,
  nexora.workflow_action
FROM PUBLIC;
GRANT USAGE ON TYPE
  nexora.page_state,
  nexora.theme_version_state,
  nexora.publication_operation,
  nexora.workflow_action
TO nexora_runtime;

DO $$
DECLARE
  api_role text;
BEGIN
  FOREACH api_role IN ARRAY ARRAY['anon', 'authenticated', 'service_role']
  LOOP
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = api_role) THEN
      EXECUTE format(
        'REVOKE USAGE ON TYPE nexora.page_state, nexora.theme_version_state, nexora.publication_operation, nexora.workflow_action FROM %I',
        api_role
      );
    END IF;
  END LOOP;
END
$$;

CREATE TABLE nexora.sites (
  id uuid PRIMARY KEY,
  organization_id uuid NOT NULL
    REFERENCES nexora.organizations (id)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  slug text NOT NULL CHECK (slug ~ '^[a-z][a-z0-9-]{0,119}$'),
  canonical_host text NOT NULL
    CHECK (canonical_host = lower(canonical_host))
    CHECK (canonical_host ~ '^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+$'),
  status text NOT NULL DEFAULT 'ACTIVE' CHECK (status = 'ACTIVE'),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  created_at timestamp with time zone NOT NULL DEFAULT transaction_timestamp(),
  updated_at timestamp with time zone NOT NULL DEFAULT transaction_timestamp(),
  CONSTRAINT sites_updated_after_created CHECK (updated_at >= created_at),
  CONSTRAINT sites_organization_slug_key UNIQUE (organization_id, slug),
  CONSTRAINT sites_organization_id_id_key UNIQUE (organization_id, id),
  CONSTRAINT sites_canonical_host_key UNIQUE (canonical_host)
);

CREATE TABLE nexora.themes (
  id uuid PRIMARY KEY,
  organization_id uuid NOT NULL
    REFERENCES nexora.organizations (id)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  slug text NOT NULL CHECK (slug ~ '^[a-z][a-z0-9-]{0,119}$'),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  created_at timestamp with time zone NOT NULL DEFAULT transaction_timestamp(),
  updated_at timestamp with time zone NOT NULL DEFAULT transaction_timestamp(),
  CONSTRAINT themes_updated_after_created CHECK (updated_at >= created_at),
  CONSTRAINT themes_organization_slug_key UNIQUE (organization_id, slug),
  CONSTRAINT themes_organization_id_id_key UNIQUE (organization_id, id)
);

CREATE TABLE nexora.theme_versions (
  id uuid PRIMARY KEY,
  organization_id uuid NOT NULL,
  theme_id uuid NOT NULL,
  version bigint NOT NULL CHECK (version > 0),
  state nexora.theme_version_state NOT NULL DEFAULT 'DRAFT',
  token_digest text NOT NULL CHECK (token_digest ~ '^sha256:[a-f0-9]{64}$'),
  token_manifest jsonb NOT NULL CHECK (jsonb_typeof(token_manifest) = 'object')
    CHECK (pg_column_size(token_manifest) <= 65536),
  actor_id uuid NOT NULL,
  created_at timestamp with time zone NOT NULL DEFAULT transaction_timestamp(),
  CONSTRAINT theme_versions_theme_fk
    FOREIGN KEY (organization_id, theme_id)
    REFERENCES nexora.themes (organization_id, id)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT theme_versions_organization_id_id_key UNIQUE (organization_id, id),
  CONSTRAINT theme_versions_theme_version_key UNIQUE (organization_id, theme_id, version)
);

CREATE TABLE nexora.media_assets (
  id uuid PRIMARY KEY,
  organization_id uuid NOT NULL
    REFERENCES nexora.organizations (id)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  content_digest text NOT NULL CHECK (content_digest ~ '^sha256:[a-f0-9]{64}$'),
  media_kind text NOT NULL CHECK (media_kind IN ('IMAGE', 'DOCUMENT')),
  byte_size bigint NOT NULL CHECK (byte_size > 0 AND byte_size <= 52428800),
  created_at timestamp with time zone NOT NULL DEFAULT transaction_timestamp(),
  CONSTRAINT media_assets_organization_id_id_key UNIQUE (organization_id, id),
  CONSTRAINT media_assets_organization_digest_key UNIQUE (organization_id, content_digest)
);

CREATE TABLE nexora.pages (
  id uuid PRIMARY KEY,
  organization_id uuid NOT NULL,
  site_id uuid NOT NULL,
  slug text NOT NULL CHECK (slug ~ '^[a-z0-9]+(?:-[a-z0-9]+)*$'),
  title text NOT NULL CHECK (char_length(title) BETWEEN 1 AND 160),
  state nexora.page_state NOT NULL DEFAULT 'DRAFT',
  draft_version bigint NOT NULL DEFAULT 1 CHECK (draft_version > 0),
  schema_version text NOT NULL CHECK (schema_version ~ '^[1-9][0-9]*\.[0-9]+\.[0-9]+$'),
  content_digest text NOT NULL CHECK (content_digest ~ '^sha256:[a-f0-9]{64}$'),
  theme_version_id uuid NOT NULL,
  seo_title text NOT NULL CHECK (char_length(seo_title) BETWEEN 1 AND 70),
  seo_description text NOT NULL CHECK (char_length(seo_description) BETWEEN 1 AND 160),
  seo_locale text NOT NULL CHECK (seo_locale ~ '^[a-z]{2,3}(?:-[A-Z]{2})?$'),
  canonical_path text NOT NULL CHECK (canonical_path ~ '^/[a-z0-9]+(?:[/-][a-z0-9]+)*$'),
  open_graph_title text CHECK (char_length(open_graph_title) BETWEEN 1 AND 70),
  open_graph_description text CHECK (char_length(open_graph_description) BETWEEN 1 AND 160),
  open_graph_image_asset_id uuid,
  open_graph_type text CHECK (open_graph_type IN ('website', 'article')),
  twitter_card text CHECK (twitter_card IN ('summary', 'summary_large_image')),
  twitter_title text CHECK (char_length(twitter_title) BETWEEN 1 AND 70),
  twitter_description text CHECK (char_length(twitter_description) BETWEEN 1 AND 160),
  twitter_image_asset_id uuid,
  json_ld_type text CHECK (json_ld_type IN ('WebPage', 'Article')),
  published_version_id uuid,
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  created_at timestamp with time zone NOT NULL DEFAULT transaction_timestamp(),
  updated_at timestamp with time zone NOT NULL DEFAULT transaction_timestamp(),
  CONSTRAINT pages_updated_after_created CHECK (updated_at >= created_at),
  CONSTRAINT pages_site_fk
    FOREIGN KEY (organization_id, site_id)
    REFERENCES nexora.sites (organization_id, id)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT pages_theme_version_fk
    FOREIGN KEY (organization_id, theme_version_id)
    REFERENCES nexora.theme_versions (organization_id, id)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT pages_open_graph_asset_fk
    FOREIGN KEY (organization_id, open_graph_image_asset_id)
    REFERENCES nexora.media_assets (organization_id, id)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT pages_twitter_asset_fk
    FOREIGN KEY (organization_id, twitter_image_asset_id)
    REFERENCES nexora.media_assets (organization_id, id)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT pages_organization_id_id_key UNIQUE (organization_id, id),
  CONSTRAINT pages_site_slug_key UNIQUE (organization_id, site_id, slug)
);

CREATE TABLE nexora.page_versions (
  id uuid PRIMARY KEY,
  organization_id uuid NOT NULL,
  site_id uuid NOT NULL,
  page_id uuid NOT NULL,
  source_draft_version bigint NOT NULL CHECK (source_draft_version > 0),
  publication_operation nexora.publication_operation NOT NULL,
  schema_version text NOT NULL CHECK (schema_version ~ '^[1-9][0-9]*\.[0-9]+\.[0-9]+$'),
  content_digest text NOT NULL CHECK (content_digest ~ '^sha256:[a-f0-9]{64}$'),
  theme_version_id uuid NOT NULL,
  seo_title text NOT NULL CHECK (char_length(seo_title) BETWEEN 1 AND 70),
  seo_description text NOT NULL CHECK (char_length(seo_description) BETWEEN 1 AND 160),
  seo_locale text NOT NULL CHECK (seo_locale ~ '^[a-z]{2,3}(?:-[A-Z]{2})?$'),
  canonical_path text NOT NULL CHECK (canonical_path ~ '^/[a-z0-9]+(?:[/-][a-z0-9]+)*$'),
  open_graph_title text CHECK (char_length(open_graph_title) BETWEEN 1 AND 70),
  open_graph_description text CHECK (char_length(open_graph_description) BETWEEN 1 AND 160),
  open_graph_image_asset_id uuid,
  open_graph_type text CHECK (open_graph_type IN ('website', 'article')),
  twitter_card text CHECK (twitter_card IN ('summary', 'summary_large_image')),
  twitter_title text CHECK (char_length(twitter_title) BETWEEN 1 AND 70),
  twitter_description text CHECK (char_length(twitter_description) BETWEEN 1 AND 160),
  twitter_image_asset_id uuid,
  json_ld_type text CHECK (json_ld_type IN ('WebPage', 'Article')),
  seo_snapshot_digest text NOT NULL CHECK (seo_snapshot_digest ~ '^sha256:[a-f0-9]{64}$'),
  actor_id uuid NOT NULL,
  trace_id text NOT NULL CHECK (trace_id ~ '^[A-Za-z0-9._-]{1,128}$'),
  created_at timestamp with time zone NOT NULL DEFAULT transaction_timestamp(),
  CONSTRAINT page_versions_page_fk
    FOREIGN KEY (organization_id, page_id)
    REFERENCES nexora.pages (organization_id, id)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT page_versions_site_fk
    FOREIGN KEY (organization_id, site_id)
    REFERENCES nexora.sites (organization_id, id)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT page_versions_theme_version_fk
    FOREIGN KEY (organization_id, theme_version_id)
    REFERENCES nexora.theme_versions (organization_id, id)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT page_versions_open_graph_asset_fk
    FOREIGN KEY (organization_id, open_graph_image_asset_id)
    REFERENCES nexora.media_assets (organization_id, id)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT page_versions_twitter_asset_fk
    FOREIGN KEY (organization_id, twitter_image_asset_id)
    REFERENCES nexora.media_assets (organization_id, id)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT page_versions_organization_id_id_key UNIQUE (organization_id, id),
  CONSTRAINT page_versions_page_source_draft_key UNIQUE (organization_id, page_id, source_draft_version, publication_operation)
);

ALTER TABLE nexora.pages
  ADD CONSTRAINT pages_published_version_fk
  FOREIGN KEY (organization_id, published_version_id)
  REFERENCES nexora.page_versions (organization_id, id)
  ON UPDATE RESTRICT ON DELETE RESTRICT
  DEFERRABLE INITIALLY DEFERRED;

CREATE TABLE nexora.page_publications (
  id uuid PRIMARY KEY,
  organization_id uuid NOT NULL,
  site_id uuid NOT NULL,
  page_id uuid NOT NULL,
  source_draft_version bigint NOT NULL CHECK (source_draft_version > 0),
  published_version_id uuid NOT NULL,
  publication_operation nexora.publication_operation NOT NULL,
  actor_id uuid NOT NULL,
  trace_id text NOT NULL CHECK (trace_id ~ '^[A-Za-z0-9._-]{1,128}$'),
  idempotency_key_digest text NOT NULL CHECK (idempotency_key_digest ~ '^sha256:[a-f0-9]{64}$'),
  request_fingerprint text NOT NULL CHECK (request_fingerprint ~ '^sha256:[a-f0-9]{64}$'),
  created_at timestamp with time zone NOT NULL DEFAULT transaction_timestamp(),
  CONSTRAINT page_publications_page_fk
    FOREIGN KEY (organization_id, page_id)
    REFERENCES nexora.pages (organization_id, id)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT page_publications_site_fk
    FOREIGN KEY (organization_id, site_id)
    REFERENCES nexora.sites (organization_id, id)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT page_publications_version_fk
    FOREIGN KEY (organization_id, published_version_id)
    REFERENCES nexora.page_versions (organization_id, id)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT page_publications_published_version_key UNIQUE (organization_id, published_version_id),
  CONSTRAINT page_publications_idempotency_key UNIQUE (organization_id, page_id, publication_operation, idempotency_key_digest)
);

CREATE TABLE nexora.workflow_reviews (
  id uuid PRIMARY KEY,
  organization_id uuid NOT NULL,
  page_id uuid NOT NULL,
  candidate_draft_version bigint NOT NULL CHECK (candidate_draft_version > 0),
  action nexora.workflow_action NOT NULL,
  from_state nexora.page_state NOT NULL,
  to_state nexora.page_state NOT NULL,
  reason text CHECK (char_length(reason) BETWEEN 1 AND 512),
  actor_id uuid NOT NULL,
  trace_id text NOT NULL CHECK (trace_id ~ '^[A-Za-z0-9._-]{1,128}$'),
  created_at timestamp with time zone NOT NULL DEFAULT transaction_timestamp(),
  CONSTRAINT workflow_reviews_page_fk
    FOREIGN KEY (organization_id, page_id)
    REFERENCES nexora.pages (organization_id, id)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT workflow_reviews_rejection_reason
    CHECK ((action = 'REJECT') = (reason IS NOT NULL))
);

CREATE TABLE nexora.cms_audit_events (
  id uuid PRIMARY KEY,
  organization_id uuid NOT NULL,
  site_id uuid,
  page_id uuid,
  version_id uuid,
  operation text NOT NULL CHECK (operation IN ('PAGE_CREATE', 'PAGE_UPDATE', 'WORKFLOW', 'PUBLISH', 'ROLLBACK', 'THEME_PUBLISH')),
  result text NOT NULL CHECK (result IN ('ACCEPTED', 'DENIED', 'CONFLICT', 'REJECTED')),
  actor_id uuid NOT NULL,
  trace_id text NOT NULL CHECK (trace_id ~ '^[A-Za-z0-9._-]{1,128}$'),
  idempotency_key_digest text CHECK (idempotency_key_digest ~ '^sha256:[a-f0-9]{64}$'),
  created_at timestamp with time zone NOT NULL DEFAULT transaction_timestamp(),
  CONSTRAINT cms_audit_events_site_fk
    FOREIGN KEY (organization_id, site_id)
    REFERENCES nexora.sites (organization_id, id)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT cms_audit_events_page_fk
    FOREIGN KEY (organization_id, page_id)
    REFERENCES nexora.pages (organization_id, id)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT cms_audit_events_version_fk
    FOREIGN KEY (organization_id, version_id)
    REFERENCES nexora.page_versions (organization_id, id)
    ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE INDEX pages_tenant_listing_idx
ON nexora.pages (organization_id, site_id, updated_at DESC, id);
CREATE INDEX pages_published_resolution_idx
ON nexora.pages (organization_id, site_id, slug)
WHERE state = 'PUBLISHED';
CREATE INDEX page_versions_page_history_idx
ON nexora.page_versions (organization_id, page_id, created_at DESC, id);
CREATE INDEX page_publications_page_created_idx
ON nexora.page_publications (organization_id, page_id, created_at DESC, id);
CREATE INDEX workflow_reviews_page_created_idx
ON nexora.workflow_reviews (organization_id, page_id, created_at DESC, id);
CREATE INDEX cms_audit_events_page_created_idx
ON nexora.cms_audit_events (organization_id, page_id, created_at DESC, id);

CREATE TRIGGER sites_advance_version
BEFORE UPDATE ON nexora.sites
FOR EACH ROW EXECUTE FUNCTION nexora.advance_row_version();
CREATE TRIGGER themes_advance_version
BEFORE UPDATE ON nexora.themes
FOR EACH ROW EXECUTE FUNCTION nexora.advance_row_version();
CREATE TRIGGER pages_advance_version
BEFORE UPDATE ON nexora.pages
FOR EACH ROW EXECUTE FUNCTION nexora.advance_row_version();

CREATE FUNCTION nexora.reject_immutable_cms_history()
RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = pg_catalog, nexora
AS $function$
BEGIN
  RAISE EXCEPTION USING
    ERRCODE = '55000',
    MESSAGE = 'immutable CMS history requires a new version or forward correction';
END
$function$;

REVOKE ALL ON FUNCTION nexora.reject_immutable_cms_history() FROM PUBLIC;

CREATE TRIGGER theme_versions_immutable
BEFORE UPDATE OR DELETE ON nexora.theme_versions
FOR EACH ROW EXECUTE FUNCTION nexora.reject_immutable_cms_history();
CREATE TRIGGER page_versions_immutable
BEFORE UPDATE OR DELETE ON nexora.page_versions
FOR EACH ROW EXECUTE FUNCTION nexora.reject_immutable_cms_history();
CREATE TRIGGER page_publications_immutable
BEFORE UPDATE OR DELETE ON nexora.page_publications
FOR EACH ROW EXECUTE FUNCTION nexora.reject_immutable_cms_history();
CREATE TRIGGER workflow_reviews_immutable
BEFORE UPDATE OR DELETE ON nexora.workflow_reviews
FOR EACH ROW EXECUTE FUNCTION nexora.reject_immutable_cms_history();
CREATE TRIGGER cms_audit_events_immutable
BEFORE UPDATE OR DELETE ON nexora.cms_audit_events
FOR EACH ROW EXECUTE FUNCTION nexora.reject_immutable_cms_history();

ALTER TABLE nexora.sites ENABLE ROW LEVEL SECURITY;
ALTER TABLE nexora.sites FORCE ROW LEVEL SECURITY;
ALTER TABLE nexora.themes ENABLE ROW LEVEL SECURITY;
ALTER TABLE nexora.themes FORCE ROW LEVEL SECURITY;
ALTER TABLE nexora.theme_versions ENABLE ROW LEVEL SECURITY;
ALTER TABLE nexora.theme_versions FORCE ROW LEVEL SECURITY;
ALTER TABLE nexora.media_assets ENABLE ROW LEVEL SECURITY;
ALTER TABLE nexora.media_assets FORCE ROW LEVEL SECURITY;
ALTER TABLE nexora.pages ENABLE ROW LEVEL SECURITY;
ALTER TABLE nexora.pages FORCE ROW LEVEL SECURITY;
ALTER TABLE nexora.page_versions ENABLE ROW LEVEL SECURITY;
ALTER TABLE nexora.page_versions FORCE ROW LEVEL SECURITY;
ALTER TABLE nexora.page_publications ENABLE ROW LEVEL SECURITY;
ALTER TABLE nexora.page_publications FORCE ROW LEVEL SECURITY;
ALTER TABLE nexora.workflow_reviews ENABLE ROW LEVEL SECURITY;
ALTER TABLE nexora.workflow_reviews FORCE ROW LEVEL SECURITY;
ALTER TABLE nexora.cms_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE nexora.cms_audit_events FORCE ROW LEVEL SECURITY;

COMMIT;
