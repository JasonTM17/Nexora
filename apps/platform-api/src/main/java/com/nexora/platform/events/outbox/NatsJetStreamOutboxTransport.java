package com.nexora.platform.events.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nexora.platform.observability.TraceIdPolicy;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.Nats;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** JetStream acknowledgement is the transport durability boundary for an outbox event. */
@Component
@Profile("database")
@ConditionalOnProperty(prefix = "nexora.outbox.publisher", name = "enabled", havingValue = "true")
public class NatsJetStreamOutboxTransport implements OutboxTransport {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final OutboxPublisherProperties properties;

    public NatsJetStreamOutboxTransport(OutboxPublisherProperties properties) {
        this.properties = properties;
    }

    @Override
    public void publish(OutboxEvent event) {
        try (Connection connection = Nats.connect(properties.natsUrl())) {
            JetStream jetStream = connection.jetStream();
            jetStream.publish(properties.subject(), envelope(event));
        } catch (IOException | JetStreamApiException exception) {
            throw new OutboxTransportException("JetStream did not acknowledge the outbox event.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new OutboxTransportException("JetStream publication was interrupted.", exception);
        }
    }

    private byte[] envelope(OutboxEvent event) {
        if (!TraceIdPolicy.isSafe(event.traceId())) {
            throw new OutboxTransportException("The outbox event has no safe trace identifier.", null);
        }
        try {
            JsonNode safePayload = JSON.readTree(event.safePayloadJson());
            if (!safePayload.isObject()) {
                throw new OutboxTransportException("The outbox event has no object safe payload.", null);
            }
            ObjectNode envelope = JSON.createObjectNode();
            envelope.put("eventId", event.id().toString());
            envelope.put("eventType", event.eventType());
            envelope.put("eventVersion", event.eventVersion());
            envelope.put("organizationId", event.organizationId().toString());
            envelope.put("subjectId", event.subjectId().toString());
            envelope.put("resourceType", event.resourceType());
            envelope.put("resourceId", event.resourceId().toString());
            envelope.put("topic", event.topic());
            envelope.put("actorId", event.actorId().toString());
            envelope.put("traceId", event.traceId());
            envelope.put("idempotencyKeyDigest", event.idempotencyKeyDigest());
            envelope.put("payloadDigest", event.payloadDigest());
            envelope.set("safePayload", safePayload);
            envelope.put("occurredAt", event.occurredAt().toString());
            envelope.put("schemaVersion", event.schemaVersion());
            return JSON.writeValueAsBytes(envelope);
        } catch (JsonProcessingException exception) {
            throw new OutboxTransportException("The outbox event cannot be encoded safely.", exception);
        }
    }
}
