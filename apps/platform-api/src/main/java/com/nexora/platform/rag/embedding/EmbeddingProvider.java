package com.nexora.platform.rag.embedding;

/**
 * Bounded embedding provider abstraction. Implementations must declare the
 * model id, revision and output dimensions and must never log the input text.
 * The deterministic CI provider is used by automated tests; live adapters are
 * separately labeled with model and date evidence.
 */
public interface EmbeddingProvider {
    String MODEL_ID = "qwen3-embedding-0.6b";
    int DIMENSIONS = 1024;

    default int dimensions() {
        return DIMENSIONS;
    }

    /**
     * Embeds one bounded text. The caller owns size/token ceilings; the
     * provider owns its timeout and error contract.
     */
    float[] embed(String text);

    String modelId();

    String modelRevision();
}
