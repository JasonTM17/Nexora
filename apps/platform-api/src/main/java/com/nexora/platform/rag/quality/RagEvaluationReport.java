package com.nexora.platform.rag.quality;

import java.time.Instant;

/**
 * Summary evaluation report for RAG retrieval quality and precision.
 */
public record RagEvaluationReport(
        String datasetId,
        String datasetChecksum,
        int totalQueries,
        double recallAtK,
        double citationPrecision,
        double noAnswerRate,
        double averageLatencyMs,
        String modelId,
        String modelRevision,
        boolean rerankerEnabled,
        Instant evaluatedAt,
        String notes
) {}
