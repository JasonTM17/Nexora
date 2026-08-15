package com.nexora.platform.events.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
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

    @Test
    void recordsContractViolationsSeparatelyFromTransientTransportFailures() {
        OutboxEvent fifthAttempt = event(5);
        RecordingEvents events = new RecordingEvents(fifthAttempt);
        OutboxPublisher publisher = new OutboxPublisher(
                events,
                ignored -> { throw new OutboxContractViolationException("invalid envelope", null); },
                new OutboxPublisherProperties(true, "nats://unused", "nexora.events.workflow", "test-worker",
                        Duration.ofSeconds(30), 1));

        OutboxPublisher.PublishResult result = publisher.publishAvailable();

        assertThat(result.deadLettered()).isEqualTo(1);
        assertThat(events.lastErrorCode).isEqualTo("EVENT_CONTRACT_REJECTED");
        assertThat(events.deadLetterErrorCode).isEqualTo("EVENT_CONTRACT_REJECTED");
    }

    private static OutboxEvent event(int attemptCount) {
        return new OutboxEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "page", UUID.randomUUID(), 1,
                "tenant:00000000-0000-4000-8000-000000000001:workflow", "WORKFLOW_TRANSITIONED", "1.1.0",
                "sha256:" + "1".repeat(64), "sha256:" + "2".repeat(64), "{\"resourceId\":\"00000000-0000-4000-8000-000000000005\",\"resourceType\":\"page\",\"organizationId\":\"00000000-0000-4000-8000-000000000001\",\"subjectId\":\"00000000-0000-4000-8000-000000000002\",\"actorId\":\"00000000-0000-4000-8000-000000000003\",\"eventVersion\":1,\"traceId\":\"00000000000000000000000000000001\",\"schemaVersion\":\"1.1.0\",\"safeDisplay\":{\"label\":\"WORKFLOW_TRANSITIONED\",\"status\":\"ARCHIVED\",\"variant\":\"neutral\"}}", "00000000000000000000000000000001", Instant.now(), attemptCount);
    }

    private static final class RecordingEvents extends OutboxEventRepository {
        private final OutboxEvent event;
        private UUID failedEventId;
        private UUID deadLetteredEventId;
        private String lastErrorCode;
        private String deadLetterErrorCode;

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
            lastErrorCode = errorCode;
            return event;
        }

        @Override
        public void deadLetter(UUID eventId, String errorCode) {
            deadLetteredEventId = eventId;
            deadLetterErrorCode = errorCode;
        }
    }
}
