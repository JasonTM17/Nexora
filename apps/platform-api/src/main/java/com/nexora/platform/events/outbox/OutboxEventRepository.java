package com.nexora.platform.events.outbox;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
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
                SELECT id, topic, event_type::text, schema_version, idempotency_key_digest,
                       payload_digest, safe_payload::text, attempt_count
                FROM nexora.claim_outbox_events(?, ?::interval, ?)
                """, this::map, owner, interval(lease), batchSize);
    }

    public void markPublished(UUID eventId, String owner) {
        jdbc.queryForObject("SELECT id FROM nexora.publish_claimed_outbox_event(?, ?)", UUID.class, eventId, owner);
    }

    public OutboxEvent markFailed(UUID eventId, String owner, String errorCode) {
        return jdbc.queryForObject("""
                SELECT id, topic, event_type::text, schema_version, idempotency_key_digest,
                       payload_digest, safe_payload::text, attempt_count
                FROM nexora.fail_claimed_outbox_event(?, ?, ?)
                """, this::map, eventId, owner, errorCode);
    }

    public void deadLetter(UUID eventId, String errorCode) {
        jdbc.queryForObject("SELECT id FROM nexora.dead_letter_failed_outbox_event(?, ?)", UUID.class,
                eventId, errorCode);
    }

    private OutboxEvent map(ResultSet result, int row) throws SQLException {
        return new OutboxEvent(
                result.getObject("id", UUID.class), result.getString("topic"), result.getString("event_type"),
                result.getString("schema_version"), result.getString("idempotency_key_digest"),
                result.getString("payload_digest"), result.getString("safe_payload"), result.getInt("attempt_count"));
    }

    private String interval(Duration duration) {
        if (duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("The outbox claim lease must be positive.");
        }
        return duration.toSeconds() + " seconds";
    }
}
