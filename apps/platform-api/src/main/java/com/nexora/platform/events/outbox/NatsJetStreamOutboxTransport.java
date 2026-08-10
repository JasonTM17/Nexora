package com.nexora.platform.events.outbox;

import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.Nats;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** JetStream acknowledgement is the transport durability boundary for an outbox event. */
@Component
@Profile("database")
@ConditionalOnProperty(prefix = "nexora.outbox.publisher", name = "enabled", havingValue = "true")
public class NatsJetStreamOutboxTransport implements OutboxTransport {
    private static final Pattern SAFE_TRACE_ID = Pattern.compile("[A-Za-z0-9._-]{1,128}");
    private final OutboxPublisherProperties properties;

    public NatsJetStreamOutboxTransport(OutboxPublisherProperties properties) {
        this.properties = properties;
    }

    @Override
    public void publish(OutboxEvent event) {
        try (Connection connection = Nats.connect(properties.natsUrl())) {
            JetStream jetStream = connection.jetStream();
            jetStream.publish(properties.subject(), envelope(event).getBytes(StandardCharsets.UTF_8));
        } catch (IOException | JetStreamApiException exception) {
            throw new OutboxTransportException("JetStream did not acknowledge the outbox event.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new OutboxTransportException("JetStream publication was interrupted.", exception);
        }
    }

    private String envelope(OutboxEvent event) {
        if (event.traceId() == null || !SAFE_TRACE_ID.matcher(event.traceId()).matches()) {
            throw new OutboxTransportException("The outbox event has no safe trace identifier.", null);
        }
        return """
                {"eventId":"%s","eventType":"%s","eventVersion":%d,"organizationId":"%s",
                "subjectId":"%s","resourceType":"%s","resourceId":"%s","topic":"%s",
                "actorId":"%s","traceId":"%s","idempotencyKeyDigest":"%s","payloadDigest":"%s",
                "safePayload":%s,"occurredAt":"%s","schemaVersion":"%s"}
                """.formatted(event.id(), event.eventType(), event.eventVersion(), event.organizationId(),
                event.subjectId(), event.resourceType(), event.resourceId(), event.topic(), event.actorId(),
                event.traceId(), event.idempotencyKeyDigest(), event.payloadDigest(), event.safePayloadJson(),
                DateTimeFormatter.ISO_INSTANT.format(event.occurredAt()), event.schemaVersion())
                .replaceAll("\\s+", "");
    }
}
