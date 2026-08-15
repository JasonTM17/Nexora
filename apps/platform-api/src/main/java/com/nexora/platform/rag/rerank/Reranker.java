package com.nexora.platform.rag.rerank;

import com.nexora.platform.rag.retrieval.HybridRetrievalService;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Optional reranker behind an abstraction. The default implementation returns
 * the authorized hybrid order unchanged; a live reranker may only be enabled
 * after a fixed-corpus evaluation proves a quality/cost trade-off. Failure
 * always falls back to the safe original ordering.
 */
@Service
@Profile("database")
public class Reranker {
    public List<HybridRetrievalService.FusedMatch> rerank(
            List<HybridRetrievalService.FusedMatch> candidates, String query) {
        if (candidates == null) {
            return List.of();
        }
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("The rerank query is required.");
        }
        return List.copyOf(candidates);
    }
}
