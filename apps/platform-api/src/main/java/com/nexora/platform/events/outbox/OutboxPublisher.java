package com.nexora.platform.events.outbox;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * One explicit publisher pass. Scheduling and environment activation remain
 * outside this packet, so a local database profile never starts a publisher by
 * surprise.
 */
@Service
@Profile("database")
@ConditionalOnProperty(prefix = "nexora.outbox.publisher", name = "enabled", havingValue = "true")
public class OutboxPublisher {
    private static final int MAX_ATTEMPTS = 5;
    private final OutboxEventRepository events;
    private final OutboxTransport transport;
    private final OutboxPublisherProperties properties;

    public OutboxPublisher(
            OutboxEventRepository events, OutboxTransport transport, OutboxPublisherProperties properties) {
        this.events = events;
        this.transport = transport;
        this.properties = properties;
    }

    public PublishResult publishAvailable() {
        List<OutboxEvent> claimed = events.claim(properties.claimOwner(), properties.claimLease(), properties.batchSize());
        int published = 0;
        int retrying = 0;
        int deadLettered = 0;
        int acknowledgementUncertain = 0;
        for (OutboxEvent event : claimed) {
            boolean transportAcknowledged = false;
            try {
                transport.publish(event);
                transportAcknowledged = true;
                events.markPublished(event.id(), properties.claimOwner());
                published++;
            } catch (RuntimeException exception) {
                if (transportAcknowledged) {
                    // A crash or DB acknowledgement failure after publish must remain replayable.
                    acknowledgementUncertain++;
                    continue;
                }
                OutboxEvent failed = events.markFailed(event.id(), properties.claimOwner(), "TRANSPORT_UNAVAILABLE");
                if (failed.attemptCount() >= MAX_ATTEMPTS) {
                    events.deadLetter(failed.id(), "TRANSPORT_UNAVAILABLE");
                    deadLettered++;
                } else {
                    retrying++;
                }
            }
        }
        return new PublishResult(claimed.size(), published, retrying, deadLettered, acknowledgementUncertain);
    }

    public record PublishResult(
            int claimed, int published, int retrying, int deadLettered, int acknowledgementUncertain) {
    }
}
