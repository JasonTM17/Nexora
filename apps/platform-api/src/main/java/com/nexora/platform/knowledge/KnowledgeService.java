package com.nexora.platform.knowledge;

import com.nexora.platform.auth.DomainAccessException;
import com.nexora.platform.authorization.PermissionEvaluator;
import com.nexora.platform.tenant.TenantContext;
import com.nexora.platform.tenant.TenantContextService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Tenant-scoped knowledge base and document lifecycle. Storage byte upload and
 * signed operations belong to the later storage adapter; this service owns the
 * database aggregate and the durable job transitions only.
 */
@Service
@Profile("database")
public class KnowledgeService {
    private static final String ACCEPTED_CONTENT_TYPES =
            "application/pdf,text/markdown,text/plain";
    private static final long MAX_BYTES = 52_428_800L;
    private static final int MAX_DOCUMENTS_PER_BATCH = 20;

    private final TenantContextService tenantContexts;
    private final PermissionEvaluator permissions;

    public KnowledgeService(TenantContextService tenantContexts, PermissionEvaluator permissions) {
        this.tenantContexts = tenantContexts;
        this.permissions = permissions;
    }

    public KnowledgeBaseList listKnowledgeBases(TenantContext actor, String cursor, int limit) {
        if (limit < 1 || limit > 100) {
            throw validation("The knowledge base limit is invalid.");
        }
        UUID after = parseCursor(cursor);
        return tenantContexts.withFreshTenant(actor, (authoritative, jdbc) -> {
            permissions.require(jdbc, authoritative, "knowledge.read");
            List<KnowledgeBaseView> rows = jdbc.query("""
                    SELECT id, name, description, state::text, version, updated_at
                    FROM nexora.knowledge_bases
                    WHERE organization_id = ?
                      AND state <> 'DELETED'
                      AND (?::uuid IS NULL OR id > ?::uuid)
                    ORDER BY id
                    LIMIT ?
                    """, this::mapKnowledgeBase, authoritative.organizationId(), after, after, limit + 1);
            String nextCursor = null;
            if (rows.size() > limit) {
                rows.removeLast();
                nextCursor = rows.getLast().id().toString();
            }
            return new KnowledgeBaseList(List.copyOf(rows), nextCursor);
        });
    }

    public KnowledgeBaseView createKnowledgeBase(TenantContext actor, CreateKnowledgeBaseCommand command) {
        if (command.name() == null || command.name().isBlank() || command.name().length() > 200) {
            throw validation("The knowledge base name is invalid.");
        }
        String description = command.description() == null ? "" : command.description();
        if (description.length() > 2000) {
            throw validation("The knowledge base description is invalid.");
        }
        return tenantContexts.withFreshTenant(actor, (authoritative, jdbc) -> {
            permissions.require(jdbc, authoritative, "knowledge.manage");
            UUID id = UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO nexora.knowledge_bases (id, organization_id, name, description, state, created_by_subject_id)
                    VALUES (?, ?, ?, ?, 'ACTIVE', ?)
                    """, id, authoritative.organizationId(), command.name().trim(), description,
                    authoritative.subjectId());
            return findKnowledgeBase(jdbc, id);
        });
    }

    public KnowledgeBaseView deleteKnowledgeBase(TenantContext actor, UUID knowledgeBaseId) {
        return tenantContexts.withFreshTenant(actor, (authoritative, jdbc) -> {
            permissions.require(jdbc, authoritative, "knowledge.manage");
            int updated = jdbc.update("""
                    UPDATE nexora.knowledge_bases SET state = 'DELETED'
                    WHERE id = ? AND organization_id = ? AND state <> 'DELETED'
                    """, knowledgeBaseId, authoritative.organizationId());
            if (updated == 0) {
                throw notFound("The knowledge base does not exist.");
            }
            return findKnowledgeBase(jdbc, knowledgeBaseId);
        });
    }

    public DocumentList listDocuments(TenantContext actor, UUID knowledgeBaseId, String cursor, int limit) {
        if (limit < 1 || limit > 100) {
            throw validation("The document limit is invalid.");
        }
        UUID after = parseCursor(cursor);
        return tenantContexts.withFreshTenant(actor, (authoritative, jdbc) -> {
            permissions.require(jdbc, authoritative, "knowledge.read");
            List<DocumentView> rows = jdbc.query("""
                    SELECT id, knowledge_base_id, original_name, content_type, byte_size, sha256, state::text, version, updated_at
                    FROM nexora.documents
                    WHERE organization_id = ?
                      AND knowledge_base_id = ?
                      AND state <> 'DELETED'
                      AND (?::uuid IS NULL OR id > ?::uuid)
                    ORDER BY id
                    LIMIT ?
                    """, this::mapDocument, authoritative.organizationId(), knowledgeBaseId, after, after,
                    limit + 1);
            String nextCursor = null;
            if (rows.size() > limit) {
                rows.removeLast();
                nextCursor = rows.getLast().id().toString();
            }
            return new DocumentList(List.copyOf(rows), nextCursor);
        });
    }

    public DocumentView registerDocument(TenantContext actor, RegisterDocumentCommand command) {
        if (command.knowledgeBaseId() == null || command.originalName() == null
                || command.originalName().isBlank() || command.originalName().length() > 255) {
            throw validation("The document metadata is invalid.");
        }
        if (!ACCEPTED_CONTENT_TYPES.contains(command.contentType())) {
            throw validation("The document content type is not accepted.");
        }
        if (command.byteSize() < 0 || command.byteSize() > MAX_BYTES) {
            throw validation("The document byte size is invalid.");
        }
        if (!command.sha256().matches("^[a-f0-9]{64}$")) {
            throw validation("The document digest is invalid.");
        }
        return tenantContexts.withFreshTenant(actor, (authoritative, jdbc) -> {
            permissions.require(jdbc, authoritative, "knowledge.manage");
            UUID id = UUID.randomUUID();
            String objectKey = "organizations/%s/knowledge/%s/documents/%s".formatted(
                    authoritative.organizationId(), command.knowledgeBaseId(), id);
            jdbc.update("""
                    INSERT INTO nexora.documents (
                        id, knowledge_base_id, organization_id, original_name, stored_object_key,
                        content_type, byte_size, sha256, state, uploaded_by_subject_id)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'UPLOADED', ?)
                    """, id, command.knowledgeBaseId(), authoritative.organizationId(),
                    command.originalName().trim(), objectKey, command.contentType(), command.byteSize(),
                    command.sha256(), authoritative.subjectId());
            return findDocument(jdbc, id);
        });
    }

    public DocumentView queueDocument(TenantContext actor, UUID documentId) {
        return tenantContexts.withFreshTenant(actor, (authoritative, jdbc) -> {
            permissions.require(jdbc, authoritative, "knowledge.manage");
            int updated = jdbc.update("""
                    UPDATE nexora.documents SET state = 'QUEUED'
                    WHERE id = ? AND organization_id = ? AND state = 'UPLOADED'
                    """, documentId, authoritative.organizationId());
            if (updated == 0) {
                throw validation("Only an uploaded document can be queued.");
            }
            UUID jobId = UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO nexora.document_jobs (id, document_id, organization_id, state, max_attempts)
                    VALUES (?, ?, ?, 'QUEUED', 5)
                    """, jobId, documentId, authoritative.organizationId());
            return findDocument(jdbc, documentId);
        });
    }

    public DocumentView deleteDocument(TenantContext actor, UUID documentId) {
        return tenantContexts.withFreshTenant(actor, (authoritative, jdbc) -> {
            permissions.require(jdbc, authoritative, "knowledge.manage");
            int updated = jdbc.update("""
                    UPDATE nexora.documents SET state = 'DELETED'
                    WHERE id = ? AND organization_id = ? AND state <> 'DELETED'
                    """, documentId, authoritative.organizationId());
            if (updated == 0) {
                throw notFound("The document does not exist.");
            }
            return findDocument(jdbc, documentId);
        });
    }

    private KnowledgeBaseView findKnowledgeBase(JdbcTemplate jdbc, UUID id) {
        List<KnowledgeBaseView> rows = jdbc.query("""
                SELECT id, name, description, state::text, version, updated_at
                FROM nexora.knowledge_bases
                WHERE id = ?
                """, this::mapKnowledgeBase, id);
        if (rows.isEmpty()) {
            throw notFound("The knowledge base does not exist.");
        }
        return rows.getFirst();
    }

    private DocumentView findDocument(JdbcTemplate jdbc, UUID id) {
        List<DocumentView> rows = jdbc.query("""
                SELECT id, knowledge_base_id, original_name, content_type, byte_size, sha256, state::text, version, updated_at
                FROM nexora.documents
                WHERE id = ?
                """, this::mapDocument, id);
        if (rows.isEmpty()) {
            throw notFound("The document does not exist.");
        }
        return rows.getFirst();
    }

    private KnowledgeBaseView mapKnowledgeBase(ResultSet result, int row) throws SQLException {
        return new KnowledgeBaseView(
                result.getObject("id", UUID.class),
                result.getString("name"),
                result.getString("description"),
                result.getString("state"),
                result.getLong("version"),
                result.getObject("updated_at", OffsetDateTime.class).toInstant());
    }

    private DocumentView mapDocument(ResultSet result, int row) throws SQLException {
        return new DocumentView(
                result.getObject("id", UUID.class),
                result.getObject("knowledge_base_id", UUID.class),
                result.getString("original_name"),
                result.getString("content_type"),
                result.getLong("byte_size"),
                result.getString("sha256"),
                result.getString("state"),
                result.getLong("version"),
                result.getObject("updated_at", OffsetDateTime.class).toInstant());
    }

    private UUID parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(cursor);
        } catch (IllegalArgumentException invalid) {
            throw validation("The cursor is invalid.");
        }
    }

    private DomainAccessException validation(String message) {
        return new DomainAccessException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message);
    }

    private DomainAccessException notFound(String message) {
        return new DomainAccessException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    public record KnowledgeBaseList(List<KnowledgeBaseView> items, String nextCursor) { }

    public record KnowledgeBaseView(
            UUID id, String name, String description, String state, long version, java.time.Instant updatedAt) { }

    public record DocumentList(List<DocumentView> items, String nextCursor) { }

    public record DocumentView(
            UUID id, UUID knowledgeBaseId, String originalName, String contentType, long byteSize,
            String sha256, String state, long version, java.time.Instant updatedAt) { }

    public record CreateKnowledgeBaseCommand(String name, String description) { }

    public record RegisterDocumentCommand(
            UUID knowledgeBaseId, String originalName, String contentType, long byteSize, String sha256) { }
}
