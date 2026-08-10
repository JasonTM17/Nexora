package com.nexora.platform.events.outbox;

import java.time.Instant;
import java.util.UUID;

/** Immutable event envelope returned after a database-owned claim. */
public record OutboxEvent(
        UUID id,
        UUID organizationId,
        UUID subjectId,
        UUID actorId,
        String resourceType,
        UUID resourceId,
        long eventVersion,
        String topic,
        String eventType,
        String schemaVersion,
        String idempotencyKeyDigest,
        String payloadDigest,
        String safePayloadJson,
        String traceId,
        Instant occurredAt,
        int attemptCount) {
}
