package com.nexora.platform.rag.conversation;

import com.nexora.platform.auth.DomainAccessException;
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
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Tenant plus subject scoped conversation persistence. Sending is idempotent
 * via the client message id digest; regenerate creates a new revision linked
 * through parentMessageId. Deletion propagates per the chat deletion order.
 */
@Service
@Profile("database")
public class ConversationService {
    private final TenantContextService tenantContexts;

    public ConversationService(TenantContextService tenantContexts) {
        this.tenantContexts = tenantContexts;
    }

    public SessionView createSession(TenantContext actor, String title) {
        if (title == null || title.isBlank() || title.length() > 200) {
            throw validation("The session title is invalid.");
        }
        return tenantContexts.withFreshTenant(actor, (authoritative, jdbc) -> {
            UUID id = UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO nexora.chat_sessions (id, organization_id, subject_id, title, state)
                    VALUES (?, ?, ?, ?, 'ACTIVE')
                    """, id, authoritative.organizationId(), authoritative.subjectId(), title.trim());
            return findSession(jdbc, id);
        });
    }

    public MessageView send(TenantContext actor, UUID sessionId, String clientMessageId, String content) {
        if (clientMessageId == null || clientMessageId.isBlank() || clientMessageId.length() > 128) {
            throw validation("The client message id is invalid.");
        }
        if (content == null || content.isBlank() || content.length() > 100000) {
            throw validation("The message content is invalid.");
        }
        return tenantContexts.withFreshTenant(actor, (authoritative, jdbc) -> {
            requireActiveSession(jdbc, sessionId, authoritative.organizationId(), authoritative.subjectId());
            String digest = clientMessageIdDigest(clientMessageId);
            List<MessageView> existing = jdbc.query("""
                    SELECT id, session_id, role::text, state::text, revision, content
                    FROM nexora.chat_messages
                    WHERE organization_id = ?
                      AND session_id = ?
                      AND subject_id = ?
                      AND client_message_id_digest = ?
                    """, this::mapMessage, authoritative.organizationId(), sessionId,
                    authoritative.subjectId(), digest);
            if (!existing.isEmpty()) {
                return existing.getFirst();
            }
            UUID id = UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO nexora.chat_messages (
                        id, session_id, organization_id, subject_id, client_message_id,
                        client_message_id_digest, role, state, revision, content)
                    VALUES (?, ?, ?, ?, ?, ?, 'user', 'COMPLETED', 1, ?)
                    """, id, sessionId, authoritative.organizationId(), authoritative.subjectId(),
                    clientMessageId, digest, content);
            return findMessage(jdbc, id);
        });
    }

    public MessageView completeAssistant(TenantContext actor, UUID sessionId, UUID parentMessageId,
                                         String content) {
        if (content == null || content.isBlank() || content.length() > 100000) {
            throw validation("The assistant content is invalid.");
        }
        return tenantContexts.withFreshTenant(actor, (authoritative, jdbc) -> {
            MessageRow parent = findMessageRow(jdbc, parentMessageId, authoritative.organizationId(),
                    authoritative.subjectId());
            UUID id = UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO nexora.chat_messages (
                        id, session_id, organization_id, subject_id, client_message_id,
                        client_message_id_digest, role, state, revision, parent_message_id, content)
                    VALUES (?, ?, ?, ?, ?, ?, 'assistant', 'COMPLETED', ?, ?, ?)
                    """, id, sessionId, authoritative.organizationId(), authoritative.subjectId(),
                    "assistant-" + id, clientMessageIdDigest("assistant-" + id), parent.revision() + 1,
                    parentMessageId, content);
            return findMessage(jdbc, id);
        });
    }

    public SessionView deleteSession(TenantContext actor, UUID sessionId) {
        return tenantContexts.withFreshTenant(actor, (authoritative, jdbc) -> {
            int updated = jdbc.update("""
                    UPDATE nexora.chat_sessions SET state = 'DELETED', deleted_at = transaction_timestamp()
                    WHERE id = ? AND organization_id = ? AND subject_id = ? AND state <> 'DELETED'
                    """, sessionId, authoritative.organizationId(), authoritative.subjectId());
            if (updated == 0) {
                throw notFound("The session does not exist.");
            }
            jdbc.update("""
                    UPDATE nexora.chat_messages SET state = 'DELETED', content = ''
                    WHERE session_id = ? AND organization_id = ? AND subject_id = ? AND state <> 'DELETED'
                    """, sessionId, authoritative.organizationId(), authoritative.subjectId());
            return findSession(jdbc, sessionId);
        });
    }

    public List<MessageView> history(TenantContext actor, UUID sessionId, String cursor, int limit) {
        if (limit < 1 || limit > 100) {
            throw validation("The history limit is invalid.");
        }
        UUID after = parseCursor(cursor);
        return tenantContexts.withFreshTenant(actor, (authoritative, jdbc) ->
                jdbc.query("""
                        SELECT id, session_id, role::text, state::text, revision, content
                        FROM nexora.chat_messages
                        WHERE organization_id = ?
                          AND session_id = ?
                          AND subject_id = ?
                          AND state <> 'DELETED'
                          AND (?::uuid IS NULL OR created_at > (SELECT created_at FROM nexora.chat_messages WHERE id = ?::uuid))
                        ORDER BY created_at, id
                        LIMIT ?
                        """, this::mapMessage, authoritative.organizationId(), sessionId,
                        authoritative.subjectId(), after, after, limit));
    }

    private MessageRow findMessageRow(JdbcTemplate jdbc, UUID messageId, UUID organizationId, UUID subjectId) {
        List<MessageRow> rows = jdbc.query("""
                SELECT id, session_id, revision, state::text
                FROM nexora.chat_messages
                WHERE id = ? AND organization_id = ? AND subject_id = ? AND state <> 'DELETED'
                """, this::mapMessageRow, messageId, organizationId, subjectId);
        if (rows.isEmpty()) {
            throw notFound("The parent message does not exist.");
        }
        return rows.getFirst();
    }

    private void requireActiveSession(JdbcTemplate jdbc, UUID sessionId, UUID organizationId, UUID subjectId) {
        List<UUID> active = jdbc.query("""
                SELECT id FROM nexora.chat_sessions
                WHERE id = ? AND organization_id = ? AND subject_id = ? AND state = 'ACTIVE'
                """, (result, row) -> result.getObject("id", UUID.class),
                sessionId, organizationId, subjectId);
        if (active.isEmpty()) {
            throw notFound("The session does not exist.");
        }
    }

    private MessageView findMessage(JdbcTemplate jdbc, UUID id) {
        List<MessageView> rows = jdbc.query("""
                SELECT id, session_id, role::text, state::text, revision, content
                FROM nexora.chat_messages
                WHERE id = ?
                """, this::mapMessage, id);
        if (rows.isEmpty()) {
            throw notFound("The message does not exist.");
        }
        return rows.getFirst();
    }

    private SessionView findSession(JdbcTemplate jdbc, UUID id) {
        List<SessionView> rows = jdbc.query("""
                SELECT id, title, state::text, version, updated_at
                FROM nexora.chat_sessions
                WHERE id = ?
                """, this::mapSession, id);
        if (rows.isEmpty()) {
            throw notFound("The session does not exist.");
        }
        return rows.getFirst();
    }

    private MessageView mapMessage(ResultSet result, int row) throws SQLException {
        return new MessageView(
                result.getObject("id", UUID.class),
                result.getObject("session_id", UUID.class),
                result.getString("role"),
                result.getString("state"),
                result.getInt("revision"),
                result.getString("content"));
    }

    private MessageRow mapMessageRow(ResultSet result, int row) throws SQLException {
        return new MessageRow(
                result.getObject("id", UUID.class),
                result.getObject("session_id", UUID.class),
                result.getInt("revision"),
                result.getString("state"));
    }

    private SessionView mapSession(ResultSet result, int row) throws SQLException {
        return new SessionView(
                result.getObject("id", UUID.class),
                result.getString("title"),
                result.getString("state"),
                result.getLong("version"),
                result.getObject("updated_at", OffsetDateTime.class).toInstant());
    }

    private static String clientMessageIdDigest(String clientMessageId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] raw = digest.digest(clientMessageId.getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(raw);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable.", impossible);
        }
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

    private record MessageRow(UUID id, UUID sessionId, int revision, String state) {
    }

    public record SessionView(UUID id, String title, String state, long version, java.time.Instant updatedAt) {
    }

    public record MessageView(UUID id, UUID sessionId, String role, String state, int revision, String content) {
    }
}
