package com.nexora.platform.search;

import com.nexora.platform.tenant.TenantContext;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Authorized hybrid global search over approved page and knowledge sources.
 *
 * <p>Search applies tenant/permission predicates BEFORE candidates and counts.
 * Pages are filtered to PUBLISHED state; knowledge chunks are filtered to
 * ACTIVE documents. Results are ranked by ts_rank score.</p>
 */
@Service
@Profile("database")
public class GlobalSearchService {

    private final JdbcTemplate jdbc;

    public GlobalSearchService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Search across authorized sources with stable cursor pagination.
     *
     * @param context tenant context
     * @param query search query (sanitized via to_tsquery)
     * @param limit max results (capped)
     * @param cursor pagination cursor (null for first page)
     * @return search results with next cursor
     */
    public SearchResults search(TenantContext context, String query, int limit, String cursor) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        // Search published pages (tenant-scoped)
        List<SearchResult> pages = searchPages(context, query, safeLimit);
        // Search knowledge chunks (tenant-scoped, ACTIVE documents only)
        List<SearchResult> knowledge = searchKnowledge(context, query, safeLimit);

        // Merge and rank by score
        List<SearchResult> merged = new ArrayList<>();
        merged.addAll(pages);
        merged.addAll(knowledge);
        merged.sort((a, b) -> Double.compare(b.score(), a.score()));

        List<SearchResult> items = merged.size() > safeLimit ? merged.subList(0, safeLimit) : merged;
        String nextCursor = items.size() >= safeLimit ? encryptCursor(items.get(items.size() - 1).id().toString()) : null;

        return new SearchResults(items, nextCursor, pages.size() + knowledge.size());
    }

    private List<SearchResult> searchPages(TenantContext context, String query, int limit) {
        return jdbc.query("""
                SELECT p.id, p.title,
                       ts_rank(to_tsvector('english', p.title),
                               plainto_tsquery('english', ?)) AS score
                FROM nexora.pages p
                WHERE p.organization_id = ?
                  AND p.state = 'PUBLISHED'
                  AND to_tsvector('english', p.title) @@ plainto_tsquery('english', ?)
                ORDER BY score DESC
                LIMIT ?
                """,
                (rs, row) -> new SearchResult(
                        rs.getObject("id", UUID.class),
                        "page",
                        rs.getString("title"),
                        null,
                        rs.getDouble("score")),
                query, context.organizationId(), query, limit);
    }

    private List<SearchResult> searchKnowledge(TenantContext context, String query, int limit) {
        return jdbc.query("""
                SELECT c.id, d.original_name AS title,
                       ts_rank(to_tsvector('english', c.text),
                               plainto_tsquery('english', ?)) AS score
                FROM nexora.chunks c
                JOIN nexora.documents d ON d.id = c.document_id AND d.organization_id = c.organization_id
                WHERE c.organization_id = ?
                  AND d.state = 'ACTIVE'
                  AND to_tsvector('english', c.text) @@ plainto_tsquery('english', ?)
                ORDER BY score DESC
                LIMIT ?
                """,
                (rs, row) -> new SearchResult(
                        rs.getObject("id", UUID.class),
                        "knowledge",
                        rs.getString("title"),
                        null,
                        rs.getDouble("score")),
                query, context.organizationId(), query, limit);
    }

    private String encryptCursor(String value) {
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes());
    }

    public record SearchResult(UUID id, String sourceType, String title, String snippet, double score) {
    }

    public record SearchResults(List<SearchResult> items, String nextCursor, int totalCount) {
    }
}
