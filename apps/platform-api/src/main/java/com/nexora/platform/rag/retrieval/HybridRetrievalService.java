package com.nexora.platform.rag.retrieval;

import com.nexora.platform.rag.vector.VectorService;
import com.nexora.platform.tenant.TenantContext;
import com.nexora.platform.tenant.TenantContextService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Hybrid retrieval with deterministic reciprocal-rank fusion. The lexical
 * branch uses PostgreSQL full-text search over ACTIVE chunks; the vector
 * branch delegates to the vector service. Both branches apply the identical
 * tenant plus ACTIVE-chunk predicate, and fusion is versioned so ranking
 * stays reproducible for the same corpus and query.
 */
@Service
@Profile("database")
public class HybridRetrievalService {
    public static final String FUSION_VERSION = "nexora-rrf-v1";
    private static final int RRF_K = 60;
    private static final int LEXICAL_TOP_K = 40;
    private static final int VECTOR_TOP_K = 40;
    private static final int FINAL_TOP_K = 10;

    private final TenantContextService tenantContexts;
    private final VectorService vectors;

    public HybridRetrievalService(TenantContextService tenantContexts, VectorService vectors) {
        this.tenantContexts = tenantContexts;
        this.vectors = vectors;
    }

    public RetrievalResult retrieve(TenantContext actor, String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("The retrieval query is required.");
        }
        return tenantContexts.withFreshTenant(actor, (authoritative, jdbc) -> {
            List<BranchMatch> lexical = lexical(jdbc, authoritative.organizationId(), query, LEXICAL_TOP_K);
            List<BranchMatch> vector = new ArrayList<>();
            for (VectorService.VectorMatch match : vectors.search(authoritative, query, VECTOR_TOP_K)) {
                vector.add(new BranchMatch(match.chunkId(), match.documentId(), match.similarity()));
            }
            List<FusedMatch> fused = fuse(lexical, vector);
            return new RetrievalResult(FUSION_VERSION, List.copyOf(fused), lexical.isEmpty(), vector.isEmpty());
        });
    }

    private List<BranchMatch> lexical(JdbcTemplate jdbc, UUID organizationId, String query, int topK) {
        return jdbc.query("""
                SELECT c.id, c.document_id,
                       ts_rank(to_tsvector('simple', c.text), plainto_tsquery('simple', ?)) AS rank
                FROM nexora.chunks AS c
                JOIN nexora.documents AS d
                  ON d.id = c.document_id AND d.organization_id = c.organization_id
                WHERE c.organization_id = ?
                  AND c.state = 'ACTIVE'
                  AND d.state <> 'DELETED'
                  AND to_tsvector('simple', c.text) @@ plainto_tsquery('simple', ?)
                ORDER BY rank DESC, c.id
                LIMIT ?
                """, this::mapLexical, query, organizationId, query, topK);
    }

    private BranchMatch mapLexical(ResultSet result, int row) throws SQLException {
        return new BranchMatch(
                result.getObject("id", UUID.class),
                result.getObject("document_id", UUID.class),
                result.getDouble("rank"));
    }

    static List<FusedMatch> fuse(List<BranchMatch> lexical, List<BranchMatch> vector) {
        Map<UUID, FusedMatch> byChunk = new LinkedHashMap<>();
        addBranch(byChunk, lexical, "lexical");
        addBranch(byChunk, vector, "vector");
        return byChunk.values().stream()
                .sorted((left, right) -> Double.compare(right.score(), left.score()))
                .limit(FINAL_TOP_K)
                .toList();
    }

    private static void addBranch(Map<UUID, FusedMatch> byChunk, List<BranchMatch> matches, String source) {
        int rank = 1;
        for (BranchMatch match : matches) {
            FusedMatch existing = byChunk.get(match.chunkId());
            double contribution = 1.0 / (RRF_K + rank);
            if (existing == null) {
                byChunk.put(match.chunkId(),
                        new FusedMatch(match.chunkId(), match.documentId(), contribution, source));
            } else {
                byChunk.put(match.chunkId(), new FusedMatch(
                        existing.chunkId(), existing.documentId(), existing.score() + contribution,
                        existing.sources() + "+" + source));
            }
            rank++;
        }
    }

    public record BranchMatch(UUID chunkId, UUID documentId, double score) {
    }

    public record FusedMatch(UUID chunkId, UUID documentId, double score, String sources) {
    }

    public record RetrievalResult(String fusionVersion, List<FusedMatch> matches,
                                  boolean lexicalEmpty, boolean vectorEmpty) {
    }
}
