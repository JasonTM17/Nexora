package com.nexora.platform.rag.vector;

import com.nexora.platform.auth.DomainAccessException;
import com.nexora.platform.rag.embedding.EmbeddingProvider;
import com.nexora.platform.tenant.TenantContext;
import com.nexora.platform.tenant.TenantContextService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Persists tenant-scoped chunk vectors with model/dimension provenance and
 * runs authorized similarity queries. The vector table stores only the
 * embedding and identity columns; text never enters this path.
 */
@Service
@Profile("database")
public class VectorService {
    private final TenantContextService tenantContexts;
    private final EmbeddingProvider embeddings;

    public VectorService(TenantContextService tenantContexts, EmbeddingProvider embeddings) {
        this.tenantContexts = tenantContexts;
        this.embeddings = embeddings;
    }

    public VectorReceipt embedAndStore(TenantContext actor, UUID chunkId) {
        return tenantContexts.withFreshTenant(actor, (authoritative, jdbc) -> {
            ChunkRow chunk = loadChunk(jdbc, chunkId, authoritative.organizationId());
            float[] vector = embeddings.embed(chunk.text());
            if (vector.length != EmbeddingProvider.DIMENSIONS) {
                throw new IllegalStateException("The embedding dimension does not match the stored contract.");
            }
            UUID vectorId = UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO rag.chunk_vectors (
                        id, chunk_id, document_id, organization_id, knowledge_base_id,
                        model_id, model_revision, dimensions, embedding, sha256, state)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::vector, ?, 'ACTIVE')
                    """, vectorId, chunk.id(), chunk.documentId(), authoritative.organizationId(),
                    chunk.knowledgeBaseId(), embeddings.modelId(), embeddings.modelRevision(),
                    EmbeddingProvider.DIMENSIONS, formatVector(vector), chunk.sha256());
            return new VectorReceipt(vectorId, chunkId, embeddings.modelId(), embeddings.modelRevision());
        });
    }

    public List<VectorMatch> search(TenantContext actor, String query, int topK) {
        if (query == null || query.isBlank()) {
            throw validation("The query text is required.");
        }
        if (topK < 1 || topK > 40) {
            throw validation("The top-K limit is invalid.");
        }
        float[] queryVector = embeddings.embed(query);
        return tenantContexts.withFreshTenant(actor, (authoritative, jdbc) -> {
            return jdbc.query("""
                    SELECT chunk_id, document_id, 1 - (embedding <=> ?::vector) AS similarity
                    FROM rag.chunk_vectors
                    WHERE organization_id = ?
                      AND state = 'ACTIVE'
                      AND EXISTS (
                          SELECT 1 FROM nexora.chunks AS c
                          WHERE c.id = rag.chunk_vectors.chunk_id
                            AND c.organization_id = rag.chunk_vectors.organization_id
                            AND c.state = 'ACTIVE'
                      )
                    ORDER BY embedding <=> ?::vector
                    LIMIT ?
                    """, this::mapMatch, formatVector(queryVector), authoritative.organizationId(),
                    formatVector(queryVector), topK);
        });
    }

    private ChunkRow loadChunk(JdbcTemplate jdbc, UUID chunkId, UUID organizationId) {
        List<ChunkRow> rows = jdbc.query("""
                SELECT id, document_id, knowledge_base_id, text, sha256
                FROM nexora.chunks
                WHERE id = ? AND organization_id = ? AND state = 'ACTIVE'
                """, this::mapChunk, chunkId, organizationId);
        if (rows.isEmpty()) {
            throw validation("Only an active chunk can be embedded.");
        }
        return rows.getFirst();
    }

    private ChunkRow mapChunk(ResultSet result, int row) throws SQLException {
        return new ChunkRow(
                result.getObject("id", UUID.class),
                result.getObject("document_id", UUID.class),
                result.getObject("knowledge_base_id", UUID.class),
                result.getString("text"),
                result.getString("sha256"));
    }

    private VectorMatch mapMatch(ResultSet result, int row) throws SQLException {
        return new VectorMatch(
                result.getObject("chunk_id", UUID.class),
                result.getObject("document_id", UUID.class),
                result.getDouble("similarity"));
    }

    private String formatVector(float[] vector) {
        StringBuilder formatted = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                formatted.append(',');
            }
            formatted.append(vector[i]);
        }
        return formatted.append(']').toString();
    }

    private DomainAccessException validation(String message) {
        return new DomainAccessException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message);
    }

    private record ChunkRow(UUID id, UUID documentId, UUID knowledgeBaseId, String text, String sha256) {
    }

    public record VectorReceipt(UUID vectorId, UUID chunkId, String modelId, String modelRevision) {
    }

    public record VectorMatch(UUID chunkId, UUID documentId, double similarity) {
    }
}
