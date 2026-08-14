package com.nexora.platform.events.outbox;

import com.nexora.platform.tenant.TenantContext;
import java.time.Instant;
import java.sql.Timestamp;
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

    public UUID recordArchivedPage(
            JdbcTemplate jdbc, TenantContext actor, UUID pageId, long pageVersion, Instant occurredAt) {
        EventContractV1_1.PreparedWorkflowEvent event = EventContractV1_1.archivedPage(actor, pageId, pageVersion);
        UUID eventId = UUID.randomUUID();
        UUID recorded = jdbc.queryForObject("""
                SELECT nexora.record_outbox_event(
                    ?, ?, ?, ?, ?, ?, ?::nexora.outbox_event_type, ?, ?, ?,
                    ?, ?, ?, ?::jsonb, ?)
                """, UUID.class,
                eventId, actor.organizationId(), actor.subjectId(), actor.subjectId(),
                EventContractV1_1.RESOURCE_TYPE, pageId, EventContractV1_1.EVENT_TYPE, pageVersion,
                event.topic(), EventContractV1_1.SCHEMA_VERSION, event.idempotencyKeyDigest(),
                event.idempotencyKeyDigest(), event.payloadDigest(), event.safePayloadJson(), Timestamp.from(occurredAt));
        if (recorded == null) {
            throw new IllegalStateException("The database did not return an outbox event identifier.");
        }
        return recorded;
    }

}
