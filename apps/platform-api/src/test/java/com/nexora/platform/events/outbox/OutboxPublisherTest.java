package com.nexora.platform.events.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OutboxPublisherTest {

    @Test
    void deadLettersTheFifthFailedPublishInsteadOfSchedulingAnotherRetry() {
        OutboxEvent fifthAttempt = event(5);
        RecordingEvents events = new RecordingEvents(fifthAttempt);
        OutboxPublisher publisher = new OutboxPublisher(
                events,
                ignored -> { throw new IllegalStateException("transport unavailable"); },
                new OutboxPublisherProperties(true, "nats://unused", "nexora.events.workflow", "test-worker",
                        Duration.ofSeconds(30), 1));

        OutboxPublisher.PublishResult result = publisher.publishAvailable();

        assertThat(result.claimed()).isEqualTo(1);
        assertThat(result.published()).isZero();
        assertThat(result.retrying()).isZero();
        assertThat(result.deadLettered()).isEqualTo(1);
        assertThat(events.failedEventId).isEqualTo(fifthAttempt.id());
        assertThat(events.deadLetteredEventId).isEqualTo(fifthAttempt.id());
    }

    private static OutboxEvent event(int attemptCount) {
        return new OutboxEvent(
                UUID.randomUUID(), "tenant:00000000-0000-0000-0000-000000000001:workflow", "WORKFLOW_TRANSITIONED",
                "1.0.0", "sha256:idempotency", "sha256:payload", "{\"safeDisplay\":{}}", attemptCount);
    }

    private static final class RecordingEvents extends OutboxEventRepository {
        private final OutboxEvent event;
        private UUID failedEventId;
        private UUID deadLetteredEventId;

        private RecordingEvents(OutboxEvent event) {
            super(null);
            this.event = event;
        }

        @Override
        public List<OutboxEvent> claim(String owner, Duration lease, int batchSize) {
            return List.of(event);
        }

        @Override
        public OutboxEvent markFailed(UUID eventId, String owner, String errorCode) {
            failedEventId = eventId;
            return event;
        }

        @Override
        public void deadLetter(UUID eventId, String errorCode) {
            deadLetteredEventId = eventId;
        }
    }
}
