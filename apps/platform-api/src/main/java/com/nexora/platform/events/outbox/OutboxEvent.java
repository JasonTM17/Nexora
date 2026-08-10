package com.nexora.platform.events.outbox;

import java.util.UUID;

/** Immutable event envelope returned after a database-owned claim. */
public record OutboxEvent(
        UUID id,
        String topic,
        String eventType,
        String schemaVersion,
        String idempotencyKeyDigest,
        String payloadDigest,
        String safePayloadJson,
        int attemptCount) {
}
