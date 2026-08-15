package com.nexora.platform.rag.quality;

import java.time.Instant;
import java.util.UUID;

/**
 * Tenant-scoped user feedback for a RAG retrieval run.
 */
public record RagFeedback(
        UUID id,
        UUID runId,
        UUID organizationId,
        UUID subjectId,
        String rating,
        String comment,
        Instant createdAt
) {}
