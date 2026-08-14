package com.nexora.platform.knowledge.ingestion;

import com.nexora.platform.auth.DomainAccessException;
import com.nexora.platform.tenant.TenantContext;
import com.nexora.platform.tenant.TenantContextService;
import java.io.ByteArrayInputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Runs one bounded ingestion job: claim, extract, chunk, persist chunks and
 * transition the document to READY. The worker contract lives in M4-T01's
 * durable document_jobs rows; this service owns only the processing step.
 */
@Service
@Profile("database")
public class DocumentIngestionService {
    private final TenantContextService tenantContexts;
    private final ChunkingStrategy chunking;

    public DocumentIngestionService(TenantContextService tenantContexts, ChunkingStrategy chunking) {
        this.tenantContexts = tenantContexts;
        this.chunking = chunking;
    }

    public IngestionResult process(TenantContext actor, UUID documentId, byte[] bytes) {
        if (documentId == null || bytes == null || bytes.length == 0) {
            throw validation("The ingestion inputs are required.");
        }
        return tenantContexts.withFreshTenant(actor, (authoritative, jdbc) -> {
            DocumentRow document = loadQueued(jdbc, documentId, authoritative.organizationId());
            DocumentExtractors.Extraction extraction = DocumentExtractors.extract(document.contentType(),
                    new ByteArrayInputStream(bytes));
            List<ChunkingStrategy.Chunk> chunks = chunking.chunk(extraction.text());
            if (chunks.isEmpty()) {
                throw validation("The document produced no chunks.");
            }
            jdbc.update("""
                    UPDATE nexora.documents SET state = 'CHUNKING'
                    WHERE id = ? AND organization_id = ? AND state = 'QUEUED'
                    """, documentId, authoritative.organizationId());
            int index = 0;
            for (ChunkingStrategy.Chunk chunk : chunks) {
                jdbc.update("""
                        INSERT INTO nexora.chunks (
                            id, document_id, organization_id, knowledge_base_id, chunk_index, text,
                            token_count, source_page_start, source_page_end, sha256, chunk_strategy_version, state)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE')
                        """, UUID.randomUUID(), documentId, authoritative.organizationId(),
                        document.knowledgeBaseId(), index++, chunk.text(),
                        ChunkingStrategy.estimateTokens(chunk.text()),
                        extraction.pageStart(), extraction.pageEnd(), chunk.sha256(),
                        ChunkingStrategy.STRATEGY_VERSION);
            }
            jdbc.update("""
                    UPDATE nexora.documents SET state = 'READY'
                    WHERE id = ? AND organization_id = ? AND state = 'CHUNKING'
                    """, documentId, authoritative.organizationId());
            return new IngestionResult(documentId, chunks.size());
        });
    }

    private DocumentRow loadQueued(JdbcTemplate jdbc, UUID documentId, UUID organizationId) {
        List<DocumentRow> rows = jdbc.query("""
                SELECT id, knowledge_base_id, organization_id, content_type, sha256, state::text
                FROM nexora.documents
                WHERE id = ? AND organization_id = ? AND state = 'QUEUED'
                """, this::mapDocument, documentId, organizationId);
        if (rows.isEmpty()) {
            throw validation("Only a queued document can be ingested.");
        }
        return rows.getFirst();
    }

    private DocumentRow mapDocument(ResultSet result, int row) throws SQLException {
        return new DocumentRow(
                result.getObject("id", UUID.class),
                result.getObject("knowledge_base_id", UUID.class),
                result.getObject("organization_id", UUID.class),
                result.getString("content_type"),
                result.getString("sha256"));
    }

    private DomainAccessException validation(String message) {
        return new DomainAccessException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message);
    }

    private record DocumentRow(
            UUID id, UUID knowledgeBaseId, UUID organizationId, String contentType, String sha256) {
    }

    public record IngestionResult(UUID documentId, int chunkCount) {
    }
}
