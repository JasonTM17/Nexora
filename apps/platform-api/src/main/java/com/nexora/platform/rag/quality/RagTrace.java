package com.nexora.platform.rag.quality;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Tenant-scoped safe RAG retrieval trace record.
 * Raw prompt and source text are excluded/redacted by design.
 */
public record RagTrace(
        UUID id,
        UUID sessionId,
        UUID organizationId,
        UUID subjectId,
        String queryHash,
        String corpusVersion,
        String modelId,
        String modelRevision,
        List<UUID> candidateIds,
        List<UUID> selectedChunkIds,
        String outcome,
        long latencyMs,
        int tokenCount,
        Instant createdAt
) {}
