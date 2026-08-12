package com.nexora.platform.events.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.platform.events.outbox.EventContractV1_1;
import com.nexora.platform.events.outbox.OutboxEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Verifies an entire raw v1.1 JetStream envelope before the private ledger
 * function records it. Current membership is deliberately not re-authorized:
 * delayed, previously-authorized events must converge through their immutable
 * envelope and database idempotency boundary.
 */
@Service
@Profile("database")
public class EventLedgerConsumer {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> ENVELOPE_FIELDS = Set.of(
            "eventId", "eventType", "eventVersion", "schemaVersion", "organizationId", "subjectId",
            "resourceType", "resourceId", "topic", "actorId", "traceId", "idempotencyKeyDigest",
            "payloadDigest", "safePayload", "occurredAt");

    private final EventLedgerRepository ledger;

    public EventLedgerConsumer(EventLedgerRepository ledger) {
        this.ledger = ledger;
    }

    public EventLedgerReceipt consume(byte[] rawEnvelope) {
        return ledger.record(parse(rawEnvelope));
    }

    private OutboxEvent parse(byte[] rawEnvelope) {
        try {
            JsonNode envelope = JSON.readTree(rawEnvelope);
            if (!envelope.isObject() || !fieldNames(envelope).equals(ENVELOPE_FIELDS)) {
                throw rejected(null);
            }
            JsonNode safePayload = envelope.path("safePayload");
            if (!safePayload.isObject()) {
                throw rejected(null);
            }
            OutboxEvent event = new OutboxEvent(
                    uuid(envelope, "eventId"), uuid(envelope, "organizationId"), uuid(envelope, "subjectId"),
                    uuid(envelope, "actorId"), text(envelope, "resourceType"), uuid(envelope, "resourceId"),
                    eventVersion(envelope), text(envelope, "topic"), text(envelope, "eventType"),
                    text(envelope, "schemaVersion"), text(envelope, "idempotencyKeyDigest"),
                    text(envelope, "payloadDigest"), JSON.writeValueAsString(safePayload), text(envelope, "traceId"),
                    occurredAt(envelope), 0);
            EventContractV1_1.verifyForPublication(event);
            return event;
        } catch (EventEnvelopeRejectedException exception) {
            throw exception;
        } catch (IOException | IllegalArgumentException exception) {
            throw rejected(exception);
        }
    }

    private static Set<String> fieldNames(JsonNode object) {
        java.util.HashSet<String> fields = new java.util.HashSet<>();
        object.fieldNames().forEachRemaining(fields::add);
        return fields;
    }

    private static UUID uuid(JsonNode envelope, String field) {
        return UUID.fromString(text(envelope, field));
    }

    private static String text(JsonNode envelope, String field) {
        JsonNode value = envelope.get(field);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw rejected(null);
        }
        return value.textValue();
    }

    private static long eventVersion(JsonNode envelope) {
        JsonNode value = envelope.get("eventVersion");
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong()) {
            throw rejected(null);
        }
        return value.longValue();
    }

    private static Instant occurredAt(JsonNode envelope) {
        return Instant.parse(text(envelope, "occurredAt"));
    }

    private static EventEnvelopeRejectedException rejected(Throwable cause) {
        return new EventEnvelopeRejectedException("The raw event envelope does not satisfy contract v1.1.", cause);
    }
}
