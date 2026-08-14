package com.nexora.platform.rag.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HybridRetrievalServiceTest {

    @Test
    void fusionIsDeterministicAndRanksSharedChunksHigher() {
        UUID chunkA = UUID.randomUUID();
        UUID chunkB = UUID.randomUUID();
        UUID docA = UUID.randomUUID();
        UUID docB = UUID.randomUUID();
        List<HybridRetrievalService.BranchMatch> lexical = List.of(
                new HybridRetrievalService.BranchMatch(chunkA, docA, 0.9),
                new HybridRetrievalService.BranchMatch(chunkB, docB, 0.4));
        List<HybridRetrievalService.BranchMatch> vector = List.of(
                new HybridRetrievalService.BranchMatch(chunkA, docA, 0.8));

        List<HybridRetrievalService.FusedMatch> first = HybridRetrievalService.fuse(lexical, vector);
        List<HybridRetrievalService.FusedMatch> second = HybridRetrievalService.fuse(lexical, vector);

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSize(2);
        assertThat(first.getFirst().chunkId()).isEqualTo(chunkA);
        assertThat(first.getFirst().sources()).isEqualTo("lexical+vector");
        assertThat(first.getLast().sources()).isEqualTo("lexical");
    }

    @Test
    void fusionDegradesGracefullyWhenOneBranchIsEmpty() {
        UUID chunk = UUID.randomUUID();
        UUID doc = UUID.randomUUID();
        List<HybridRetrievalService.BranchMatch> lexical = List.of(
                new HybridRetrievalService.BranchMatch(chunk, doc, 0.7));
        List<HybridRetrievalService.FusedMatch> fused = HybridRetrievalService.fuse(lexical, List.of());
        assertThat(fused).hasSize(1);
        assertThat(fused.getFirst().sources()).isEqualTo("lexical");
    }

    @Test
    void fusionLimitsToTopK() {
        java.util.List<HybridRetrievalService.BranchMatch> lexical = new java.util.ArrayList<>();
        for (int i = 0; i < 30; i++) {
            lexical.add(new HybridRetrievalService.BranchMatch(
                    UUID.randomUUID(), UUID.randomUUID(), 1.0 - i * 0.01));
        }
        List<HybridRetrievalService.FusedMatch> fused = HybridRetrievalService.fuse(lexical, List.of());
        assertThat(fused).hasSize(10);
    }

    @Test
    void bothBranchesEmptyProducesEmptyResult() {
        List<HybridRetrievalService.FusedMatch> fused = HybridRetrievalService.fuse(List.of(), List.of());
        assertThat(fused).isEmpty();
    }
}
