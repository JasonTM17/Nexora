package com.nexora.platform.cms;

import com.nexora.platform.auth.DomainAccessException;
import com.nexora.platform.authorization.PermissionEvaluator;
import com.nexora.platform.tenant.TenantContext;
import com.nexora.platform.tenant.TenantContextService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Tenant-scoped mutable CMS page aggregate. Publication/version persistence is
 * intentionally excluded: M2-T07 owns immutable versions and receipts.
 */
@Service
@Profile("database")
public class CmsPageService {
    private final TenantContextService tenantContexts;
    private final PermissionEvaluator permissions;

    public CmsPageService(TenantContextService tenantContexts, PermissionEvaluator permissions) {
        this.tenantContexts = tenantContexts;
        this.permissions = permissions;
    }

    public PageList list(TenantContext actor, String cursor, int limit) {
        if (limit < 1 || limit > 100) {
            throw validation("The page limit is invalid.");
        }
        UUID after = parseCursor(cursor);
        return tenantContexts.withFreshTenant(actor, (authoritative, jdbc) -> {
            permissions.require(jdbc, authoritative, "page.read");
            List<PageSummary> rows = jdbc.query("""
                    SELECT id, site_id, slug, title, state::text, draft_version, updated_at
                    FROM nexora.pages
                    WHERE (?::uuid IS NULL OR id > ?::uuid)
                    ORDER BY id
                    LIMIT ?
                    """, this::mapSummary, after, after, limit + 1);
            String nextCursor = null;
            if (rows.size() > limit) {
                PageSummary extra = rows.removeLast();
                nextCursor = extra.pageId().toString();
            }
            return new PageList(List.copyOf(rows), nextCursor);
        });
    }

    public PageView get(TenantContext actor, UUID pageId) {
        return tenantContexts.withFreshTenant(actor, (authoritative, jdbc) -> {
            permissions.require(jdbc, authoritative, "page.read");
            return find(jdbc, pageId);
        });
    }

    public PageView create(TenantContext actor, CreateCommand command, String traceId) {
        requireSafe(command.seo());
        return tenantContexts.withFreshTenant(actor, (authoritative, jdbc) -> {
            permissions.require(jdbc, authoritative, "page.create");
            requireReferences(jdbc, command.siteId(), command.themeVersionId());
            if (Boolean.TRUE.equals(jdbc.queryForObject("""
                    SELECT EXISTS (
                        SELECT 1 FROM nexora.pages WHERE site_id = ? AND slug = ?
                    )
                    """, Boolean.class, command.siteId(), command.slug()))) {
                throw validation("The page slug is already in use for this site.");
            }
            UUID pageId = UUID.randomUUID();
            try {
                jdbc.update("""
                        INSERT INTO nexora.pages (
                            id, organization_id, site_id, slug, title, schema_version,
                            content_digest, theme_version_id, seo_title, seo_description,
                            seo_locale, canonical_path, open_graph_title,
                            open_graph_description, open_graph_image_asset_id,
                            open_graph_type, twitter_card, twitter_title,
                            twitter_description, twitter_image_asset_id, json_ld_type
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, pageId, authoritative.organizationId(), command.siteId(), command.slug(),
                        command.title(), command.schemaVersion(), command.contentDigest(), command.themeVersionId(),
                        command.seo().title(), command.seo().description(), command.seo().locale(),
                        command.seo().canonicalPath(), command.seo().openGraphTitle(),
                        command.seo().openGraphDescription(), command.seo().openGraphImageAssetId(),
                        command.seo().openGraphType(), command.seo().twitterCard(),
                        command.seo().twitterTitle(), command.seo().twitterDescription(),
                        command.seo().twitterImageAssetId(), command.seo().jsonLdType());
            } catch (DataIntegrityViolationException exception) {
                throw validation("The page request is not valid.");
            }
            audit(jdbc, authoritative, command.siteId(), pageId, "PAGE_CREATE", traceId);
            return find(jdbc, pageId);
        });
    }

    public PageView update(TenantContext actor, UUID pageId, UpdateCommand command, String traceId) {
        requireSafe(command.seo());
        return tenantContexts.withFreshTenant(actor, (authoritative, jdbc) -> {
            permissions.require(jdbc, authoritative, "page.update");
            requireTheme(jdbc, command.themeVersionId());
            PageView existing = find(jdbc, pageId);
            if (!"DRAFT".equals(existing.state())) {
                throw workflowDenied();
            }
            int updated = jdbc.update("""
                    UPDATE nexora.pages
                    SET title = ?, schema_version = ?, content_digest = ?, theme_version_id = ?,
                        seo_title = ?, seo_description = ?, seo_locale = ?, canonical_path = ?,
                        open_graph_title = ?, open_graph_description = ?, open_graph_image_asset_id = ?,
                        open_graph_type = ?, twitter_card = ?, twitter_title = ?,
                        twitter_description = ?, twitter_image_asset_id = ?, json_ld_type = ?,
                        draft_version = draft_version + 1, updated_at = transaction_timestamp()
                    WHERE id = ? AND draft_version = ? AND state = 'DRAFT'
                    """, command.title(), command.schemaVersion(), command.contentDigest(), command.themeVersionId(),
                    command.seo().title(), command.seo().description(), command.seo().locale(),
                    command.seo().canonicalPath(), command.seo().openGraphTitle(),
                    command.seo().openGraphDescription(), command.seo().openGraphImageAssetId(),
                    command.seo().openGraphType(), command.seo().twitterCard(), command.seo().twitterTitle(),
                    command.seo().twitterDescription(), command.seo().twitterImageAssetId(), command.seo().jsonLdType(),
                    pageId, command.expectedDraftVersion());
            if (updated != 1) {
                throw versionConflict();
            }
            audit(jdbc, authoritative, existing.siteId(), pageId, "PAGE_UPDATE", traceId);
            return find(jdbc, pageId);
        });
    }

    public ArchiveResult archive(TenantContext actor, UUID pageId, long expectedDraftVersion, String traceId) {
        return tenantContexts.withFreshTenant(actor, (authoritative, jdbc) -> {
            permissions.require(jdbc, authoritative, "page.update");
            PageView existing = find(jdbc, pageId);
            if (!"PUBLISHED".equals(existing.state())) {
                throw workflowDenied();
            }
            int updated = jdbc.update("""
                    UPDATE nexora.pages
                    SET state = 'ARCHIVED', draft_version = draft_version + 1,
                        updated_at = transaction_timestamp()
                    WHERE id = ? AND draft_version = ? AND state = 'PUBLISHED'
                    """, pageId, expectedDraftVersion);
            if (updated != 1) {
                throw versionConflict();
            }
            PageView archived = find(jdbc, pageId);
            audit(jdbc, authoritative, archived.siteId(), pageId, "PAGE_UPDATE", traceId);
            return new ArchiveResult(archived.pageId(), archived.state(), archived.updatedAt());
        });
    }

    private void requireReferences(JdbcTemplate jdbc, UUID siteId, UUID themeVersionId) {
        boolean siteExists = Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM nexora.sites WHERE id = ? AND status = 'ACTIVE')",
                Boolean.class, siteId));
        if (!siteExists) {
            throw validation("The page site is not available.");
        }
        requireTheme(jdbc, themeVersionId);
    }

    private void requireTheme(JdbcTemplate jdbc, UUID themeVersionId) {
        boolean themePublished = Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM nexora.theme_versions WHERE id = ? AND state = 'PUBLISHED')",
                Boolean.class, themeVersionId));
        if (!themePublished) {
            throw new DomainAccessException(
                    HttpStatus.BAD_REQUEST, "THEME_REFERENCE_INVALID", "The page theme reference is not available.");
        }
    }

    private PageView find(JdbcTemplate jdbc, UUID pageId) {
        return jdbc.query("""
                SELECT id, site_id, slug, title, state::text, draft_version, updated_at,
                       schema_version, content_digest, theme_version_id, seo_title,
                       seo_description, seo_locale, canonical_path, open_graph_title,
                       open_graph_description, open_graph_image_asset_id, open_graph_type,
                       twitter_card, twitter_title, twitter_description,
                       twitter_image_asset_id, json_ld_type, published_version_id
                FROM nexora.pages WHERE id = ?
                """, this::mapPage, pageId).stream().findFirst().orElseThrow(() -> denied("The page is not available."));
    }

    private PageSummary mapSummary(ResultSet result, int row) throws SQLException {
        return new PageSummary(
                result.getObject("id", UUID.class), result.getObject("site_id", UUID.class),
                result.getString("slug"), result.getString("title"), result.getString("state"),
                result.getLong("draft_version"), instant(result, "updated_at"));
    }

    private PageView mapPage(ResultSet result, int row) throws SQLException {
        return new PageView(
                result.getObject("id", UUID.class), result.getObject("site_id", UUID.class),
                result.getString("slug"), result.getString("title"), result.getString("state"),
                result.getLong("draft_version"), instant(result, "updated_at"), result.getString("schema_version"),
                result.getString("content_digest"), result.getObject("theme_version_id", UUID.class),
                new SeoSnapshot(result.getString("seo_title"), result.getString("seo_description"),
                        result.getString("seo_locale"), result.getString("canonical_path"),
                        result.getString("open_graph_title"), result.getString("open_graph_description"),
                        result.getObject("open_graph_image_asset_id", UUID.class), result.getString("open_graph_type"),
                        result.getString("twitter_card"), result.getString("twitter_title"),
                        result.getString("twitter_description"), result.getObject("twitter_image_asset_id", UUID.class),
                        result.getString("json_ld_type")), result.getObject("published_version_id", UUID.class));
    }

    private Instant instant(ResultSet result, String column) throws SQLException {
        return result.getObject(column, OffsetDateTime.class).toInstant();
    }

    private void audit(JdbcTemplate jdbc, TenantContext actor, UUID siteId, UUID pageId, String operation, String traceId) {
        jdbc.update("""
                INSERT INTO nexora.cms_audit_events (
                    id, organization_id, site_id, page_id, operation, result, actor_id, trace_id
                ) VALUES (?, ?, ?, ?, ?, 'ACCEPTED', ?, ?)
                """, UUID.randomUUID(), actor.organizationId(), siteId, pageId, operation, actor.subjectId(), traceId);
    }

    private UUID parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(cursor);
        } catch (IllegalArgumentException exception) {
            throw validation("The page cursor is invalid.");
        }
    }

    private void requireSafe(SeoSnapshot seo) {
        if (seo == null || !seo.isSafe()) {
            throw new DomainAccessException(
                    HttpStatus.BAD_REQUEST, "SEO_VALIDATION_FAILED", "The page SEO metadata is not valid.");
        }
    }

    private DomainAccessException validation(String message) {
        return new DomainAccessException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message);
    }

    private DomainAccessException versionConflict() {
        return new DomainAccessException(HttpStatus.CONFLICT, "VERSION_CONFLICT", "The page draft version is stale.");
    }

    private DomainAccessException workflowDenied() {
        return new DomainAccessException(HttpStatus.CONFLICT, "WORKFLOW_TRANSITION_DENIED", "The page lifecycle does not allow this change.");
    }

    private DomainAccessException denied(String message) {
        return new DomainAccessException(HttpStatus.FORBIDDEN, "PERMISSION_DENIED", message);
    }

    public record CreateCommand(UUID siteId, String slug, String title, String schemaVersion,
                                String contentDigest, UUID themeVersionId, SeoSnapshot seo) { }

    public record UpdateCommand(long expectedDraftVersion, String title, String schemaVersion,
                                String contentDigest, UUID themeVersionId, SeoSnapshot seo) { }

    public record SeoSnapshot(
            String title, String description, String locale, String canonicalPath,
            String openGraphTitle, String openGraphDescription, UUID openGraphImageAssetId,
            String openGraphType, String twitterCard, String twitterTitle,
            String twitterDescription, UUID twitterImageAssetId, String jsonLdType) {
        boolean isSafe() {
            return matches(title, ".{1,70}") && matches(description, ".{1,160}")
                    && matches(locale, "[a-z]{2,3}(?:-[A-Z]{2})?")
                    && matches(canonicalPath, "/[a-z0-9]+(?:[/-][a-z0-9]+)*")
                    && matches(openGraphTitle, ".{1,70}") && matches(openGraphDescription, ".{1,160}")
                    && ("website".equals(openGraphType) || "article".equals(openGraphType))
                    && ("summary".equals(twitterCard) || "summary_large_image".equals(twitterCard))
                    && matches(twitterTitle, ".{1,70}") && matches(twitterDescription, ".{1,160}")
                    && ("WebPage".equals(jsonLdType) || "Article".equals(jsonLdType));
        }

        private static boolean matches(String value, String expression) {
            return value != null && value.matches(expression);
        }
    }

    public record PageSummary(UUID pageId, UUID siteId, String slug, String title,
                              String state, long draftVersion, Instant updatedAt) { }

    public record PageList(List<PageSummary> items, String nextCursor) { }

    public record PageView(UUID pageId, UUID siteId, String slug, String title, String state,
                           long draftVersion, Instant updatedAt, String schemaVersion,
                           String contentDigest, UUID themeVersionId, SeoSnapshot seo,
                           UUID publishedVersionId) { }

    public record ArchiveResult(UUID pageId, String state, Instant archivedAt) { }
}
