-- M2-DB02 forward repair: a receipt must preserve the immutable operation
-- that produced its referenced version. A ROLLBACK cannot cite a PUBLISH
-- version (or the reverse), even when the tenant, site, page and draft match.

BEGIN;
SET LOCAL ROLE nexora_migrator;

ALTER TABLE nexora.page_versions
  ADD CONSTRAINT page_versions_exact_publication_lineage_key
  UNIQUE (
    organization_id,
    site_id,
    page_id,
    source_draft_version,
    publication_operation,
    id
  );

ALTER TABLE nexora.page_publications
  DROP CONSTRAINT page_publications_exact_version_lineage_fk;

ALTER TABLE nexora.page_publications
  ADD CONSTRAINT page_publications_exact_publication_lineage_fk
  FOREIGN KEY (
    organization_id,
    site_id,
    page_id,
    source_draft_version,
    publication_operation,
    published_version_id
  )
  REFERENCES nexora.page_versions (
    organization_id,
    site_id,
    page_id,
    source_draft_version,
    publication_operation,
    id
  )
  ON UPDATE RESTRICT ON DELETE RESTRICT;

COMMENT ON CONSTRAINT page_publications_exact_publication_lineage_fk ON nexora.page_publications IS
  'Receipt source draft, operation and published version must belong to this exact tenant, site and page.';

COMMIT;
