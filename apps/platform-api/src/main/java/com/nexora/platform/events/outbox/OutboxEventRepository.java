package com.nexora.platform.events.outbox;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Uses only the V014 function-only runtime boundary; it never mutates the table directly. */
@Repository
@Profile("database")
public class OutboxEventRepository {
    private final JdbcTemplate jdbc;

    public OutboxEventRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<OutboxEvent> claim(String owner, Duration lease, int batchSize) {
        return jdbc.query("""
                SELECT id, organization_id, subject_id, actor_id, resource_type, resource_id,
                       event_version, topic, event_type::text, schema_version, idempotency_key_digest,
                       payload_digest, safe_payload::text, safe_payload ->> 'traceId' AS trace_id,
                       occurred_at, attempt_count
                FROM nexora.claim_outbox_events(?, ?::interval, ?)
                """, this::map, owner, interval(lease), batchSize);
    }

    public void markPublished(UUID eventId, String owner) {
        jdbc.queryForObject("SELECT id FROM nexora.publish_claimed_outbox_event(?, ?)", UUID.class, eventId, owner);
    }

    public OutboxEvent markFailed(UUID eventId, String owner, String errorCode) {
        return jdbc.queryForObject("""
                SELECT id, organization_id, subject_id, actor_id, resource_type, resource_id,
                       event_version, topic, event_type::text, schema_version, idempotency_key_digest,
                       payload_digest, safe_payload::text, safe_payload ->> 'traceId' AS trace_id,
                       occurred_at, attempt_count
                FROM nexora.fail_claimed_outbox_event(?, ?, ?)
                """, this::map, eventId, owner, errorCode);
    }

    /**
     * Permanently rejects a claimed event that cannot satisfy the frozen
     * contract. V021 keeps this separate from transient transport failure so
     * it can never be claimed or retried again.
     */
    public void rejectContractViolation(UUID eventId, String owner) {
        jdbc.queryForObject("SELECT id FROM nexora.reject_claimed_outbox_event(?, ?)", UUID.class,
                eventId, owner);
    }

    public void deadLetter(UUID eventId, String errorCode) {
        jdbc.queryForObject("SELECT id FROM nexora.dead_letter_failed_outbox_event(?, ?)", UUID.class,
                eventId, errorCode);
    }

    private OutboxEvent map(ResultSet result, int row) throws SQLException {
        return new OutboxEvent(
                result.getObject("id", UUID.class), result.getObject("organization_id", UUID.class),
                result.getObject("subject_id", UUID.class), result.getObject("actor_id", UUID.class),
                result.getString("resource_type"), result.getObject("resource_id", UUID.class),
                result.getLong("event_version"), result.getString("topic"), result.getString("event_type"),
                result.getString("schema_version"), result.getString("idempotency_key_digest"),
                result.getString("payload_digest"), result.getString("safe_payload"), result.getString("trace_id"),
                result.getObject("occurred_at", OffsetDateTime.class).toInstant(), result.getInt("attempt_count"));
    }

    private String interval(Duration duration) {
        if (duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("The outbox claim lease must be positive.");
        }
        return duration.toSeconds() + " seconds";
    }
}
