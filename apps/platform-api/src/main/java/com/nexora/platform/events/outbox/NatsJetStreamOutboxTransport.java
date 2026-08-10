package com.nexora.platform.events.outbox;

import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.Nats;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** JetStream acknowledgement is the transport durability boundary for an outbox event. */
@Component
@Profile("database")
@ConditionalOnProperty(prefix = "nexora.outbox.publisher", name = "enabled", havingValue = "true")
public class NatsJetStreamOutboxTransport implements OutboxTransport {
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
        return """
                {"eventId":"%s","eventType":"%s","schemaVersion":"%s","topic":"%s",
                "idempotencyKeyDigest":"%s","payloadDigest":"%s","safePayload":%s}
                """.formatted(event.id(), event.eventType(), event.schemaVersion(), event.topic(),
                event.idempotencyKeyDigest(), event.payloadDigest(), event.safePayloadJson())
                .replaceAll("\\s+", "");
    }
}
