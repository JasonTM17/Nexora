package com.nexora.platform.rag.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexora.platform.rag.retrieval.HybridRetrievalService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContextBuilderTest {

    private final ContextBuilder builder = new ContextBuilder();

    @Test
    void buildsBoundedContextWithSelectedChunkIds() {
        UUID chunk = UUID.randomUUID();
        HybridRetrievalService.RetrievalResult retrieval = new HybridRetrievalService.RetrievalResult(
                "nexora-rrf-v1",
                List.of(new HybridRetrievalService.FusedMatch(chunk, UUID.randomUUID(), 0.9, "lexical")),
                false, false, 0.9);
        ContextBuilder.ContextAssembly assembly = builder.build(retrieval, "publishing");
        assertThat(assembly.selectedChunkIds()).containsExactly(chunk);
        assertThat(assembly.context()).contains(chunk.toString());
        assertThat(assembly.estimatedTokens()).isGreaterThan(0);
    }

    @Test
    void emptyRetrievalBuildsEmptyContext() {
        HybridRetrievalService.RetrievalResult retrieval = new HybridRetrievalService.RetrievalResult(
                "nexora-rrf-v1", List.of(), true, true, Double.NEGATIVE_INFINITY);
        ContextBuilder.ContextAssembly assembly = builder.build(retrieval, "no match");
        assertThat(assembly.selectedChunkIds()).isEmpty();
        assertThat(assembly.context()).isEmpty();
    }

    @Test
    void truncatesDeterministicallyAtTokenBudget() {
        var matches = new java.util.ArrayList<HybridRetrievalService.FusedMatch>();
        for (int i = 0; i < 5000; i++) {
            matches.add(new HybridRetrievalService.FusedMatch(UUID.randomUUID(), UUID.randomUUID(), 0.5, "lexical"));
        }
        HybridRetrievalService.RetrievalResult retrieval = new HybridRetrievalService.RetrievalResult(
                "nexora-rrf-v1", matches, false, true, 0.5);
        ContextBuilder.ContextAssembly assembly = builder.build(retrieval, "large query");
        assertThat(assembly.estimatedTokens()).isLessThanOrEqualTo(8000);
        assertThat(assembly.selectedChunkIds().size()).isLessThan(matches.size());
    }
}
