package com.nexora.platform.rag.rerank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexora.platform.rag.retrieval.HybridRetrievalService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RerankerTest {

    private final Reranker reranker = new Reranker();

    @Test
    void defaultImplementationPreservesAuthorizedOrdering() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        List<HybridRetrievalService.FusedMatch> candidates = List.of(
                new HybridRetrievalService.FusedMatch(first, UUID.randomUUID(), 0.9, "lexical"),
                new HybridRetrievalService.FusedMatch(second, UUID.randomUUID(), 0.5, "vector"));
        List<HybridRetrievalService.FusedMatch> reranked = reranker.rerank(candidates, "publishing");
        assertThat(reranked).extracting(HybridRetrievalService.FusedMatch::chunkId)
                .containsExactly(first, second);
    }

    @Test
    void nullCandidatesReturnEmptyList() {
        assertThat(reranker.rerank(null, "query")).isEmpty();
    }

    @Test
    void blankQueryIsRejected() {
        assertThatThrownBy(() -> reranker.rerank(List.of(), "  "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
