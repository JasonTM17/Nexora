package com.nexora.platform.events.outbox;

import com.nexora.platform.tenant.TenantContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Records the narrow CMS workflow event through the database-owned outbox API.
 *
 * <p>The caller supplies the same transaction-scoped {@link JdbcTemplate} that
 * changed the aggregate. The function enforces the tenant context, immutable
 * envelope, topic routing, safe payload, and idempotency rules; a database
 * failure therefore rolls the aggregate change back with the outbox write.</p>
 */
@Component
@Profile("database")
public class CmsWorkflowOutboxRecorder {
    private static final String SCHEMA_VERSION = "1.0.0";
    public UUID recordArchivedPage(
            JdbcTemplate jdbc, TenantContext actor, UUID pageId, long pageVersion) {
        String topic = "tenant:%s:workflow".formatted(actor.organizationId());
        String payloadJson = """
                {"resourceId":"%s","resourceType":"page","organizationId":"%s",
                "subjectId":"%s","actorId":"%s","eventVersion":%d,"schemaVersion":"%s",
                "safeDisplay":{"label":"Page workflow","status":"ARCHIVED","state":"ARCHIVED",
                "variant":"info","hint":"Published page archived"}}
                """.formatted(pageId, actor.organizationId(), actor.subjectId(), actor.subjectId(),
                pageVersion, SCHEMA_VERSION).replaceAll("\\s+", "");

        String identity = "cms-page-archive|%s|%s|%d".formatted(
                actor.organizationId(), pageId, pageVersion);
        UUID eventId = UUID.randomUUID();
        UUID recorded = jdbc.queryForObject("""
                SELECT nexora.record_outbox_event(
                    ?, ?, ?, ?, ?, ?, ?::nexora.outbox_event_type, ?, ?, ?,
                    ?, ?, ?, ?::jsonb, transaction_timestamp())
                """, UUID.class,
                eventId, actor.organizationId(), actor.subjectId(), actor.subjectId(),
                "page", pageId, "WORKFLOW_TRANSITIONED", pageVersion, topic, SCHEMA_VERSION,
                digest(identity), digest(identity), digest(payloadJson), payloadJson);
        if (recorded == null) {
            throw new IllegalStateException("The database did not return an outbox event identifier.");
        }
        return recorded;
    }

    private static String digest(String value) {
        try {
            return "sha256:" + HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the outbox contract.", exception);
        }
    }
}
