-- M2-DB02 forward repair: a published-version pointer or receipt must bind to
-- the exact organization, site and page that own that immutable version.

BEGIN;
SET LOCAL ROLE nexora_migrator;

ALTER TABLE nexora.page_versions
  ADD CONSTRAINT page_versions_exact_page_key
  UNIQUE (organization_id, site_id, page_id, id);

ALTER TABLE nexora.page_versions
  ADD CONSTRAINT page_versions_exact_lineage_key
  UNIQUE (organization_id, site_id, page_id, source_draft_version, id);

ALTER TABLE nexora.pages
  DROP CONSTRAINT pages_published_version_fk;

ALTER TABLE nexora.pages
  ADD CONSTRAINT pages_published_version_exact_page_fk
  FOREIGN KEY (organization_id, site_id, id, published_version_id)
  REFERENCES nexora.page_versions (organization_id, site_id, page_id, id)
  ON UPDATE RESTRICT ON DELETE RESTRICT
  DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE nexora.page_publications
  DROP CONSTRAINT page_publications_version_fk;

ALTER TABLE nexora.page_publications
  ADD CONSTRAINT page_publications_exact_version_lineage_fk
  FOREIGN KEY (
    organization_id,
    site_id,
    page_id,
    source_draft_version,
    published_version_id
  )
  REFERENCES nexora.page_versions (
    organization_id,
    site_id,
    page_id,
    source_draft_version,
    id
  )
  ON UPDATE RESTRICT ON DELETE RESTRICT;

COMMENT ON CONSTRAINT pages_published_version_exact_page_fk ON nexora.pages IS
  'Published pointer must reference a version for this exact tenant, site and page.';
COMMENT ON CONSTRAINT page_publications_exact_version_lineage_fk ON nexora.page_publications IS
  'Receipt source draft and published version must belong to this exact tenant, site and page.';

COMMIT;
