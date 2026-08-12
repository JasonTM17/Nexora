package com.nexora.platform.events.consumer;

import com.nexora.platform.events.outbox.OutboxEvent;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Uses only V020/V021's function-only runtime boundary; it never writes the ledger table directly. */
@Repository
@Profile("database")
public class EventLedgerRepository {
    private final JdbcTemplate jdbc;

    public EventLedgerRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public EventLedgerReceipt record(OutboxEvent event) {
        return jdbc.queryForObject("""
                SELECT event_id, duplicate
                FROM nexora.record_event_ledger_entry(
                    ?, ?, ?, ?, ?, ?, ?::nexora.outbox_event_type, ?, ?, ?, ?, ?, ?::jsonb, ?::timestamptz
                )
                """, (result, row) -> new EventLedgerReceipt(
                result.getObject("event_id", UUID.class), result.getBoolean("duplicate")),
                event.id(), event.organizationId(), event.subjectId(), event.actorId(), event.resourceType(),
                event.resourceId(), event.eventType(), event.eventVersion(), event.topic(), event.schemaVersion(),
                event.idempotencyKeyDigest(), event.payloadDigest(), event.safePayloadJson(),
                OffsetDateTime.ofInstant(event.occurredAt(), ZoneOffset.UTC));
    }
}
