package com.nexora.platform.rag.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DeterministicEmbeddingProviderTest {

    private final DeterministicEmbeddingProvider provider = new DeterministicEmbeddingProvider();

    @Test
    void producesDeterministic1024DimensionNormalizedVectors() {
        float[] first = provider.embed("immutable publishing");
        float[] second = provider.embed("immutable publishing");
        assertThat(first).hasSize(1024);
        assertThat(first).isEqualTo(second);
        double norm = 0;
        for (float value : first) {
            norm += (double) value * value;
        }
        assertThat(Math.sqrt(norm)).isBetween(0.99, 1.01);
    }

    @Test
    void differentInputsProduceDifferentVectors() {
        float[] first = provider.embed("publishing");
        float[] second = provider.embed("rollback");
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void rejectsBlankInput() {
        assertThatThrownBy(() -> provider.embed("  ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void declaresModelAndRevision() {
        assertThat(provider.modelId()).isEqualTo("qwen3-embedding-0.6b");
        assertThat(provider.modelRevision()).isEqualTo("deterministic-ci-v1");
        assertThat(provider.dimensions()).isEqualTo(1024);
    }
}
