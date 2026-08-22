package com.nexora.platform.rag.embedding;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Deterministic CI embedding provider: hashes the input into 32 seed bytes
 * and expands them into a fixed 1024-dimension normalized vector. It is a
 * fixture, never presented as a live model result. It is the default unless
 * the live TEI adapter is enabled.
 */
@Component
@Profile("database")
@ConditionalOnProperty(
        prefix = "nexora.embedding.tei",
        name = "enabled",
        havingValue = "false",
        matchIfMissing = true)
public class DeterministicEmbeddingProvider implements EmbeddingProvider {
    private static final String REVISION = "deterministic-ci-v1";

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Embedding input text is required.");
        }
        byte[] seed = sha256(text);
        float[] vector = new float[DIMENSIONS];
        for (int i = 0; i < DIMENSIONS; i++) {
            int byteValue = seed[i % seed.length] & 0xFF;
            float value = (byteValue / 127.5f) - 1.0f;
            value = (float) (value + Math.sin(i * 12.9898 + byteValue * 78.233) * 0.1f);
            vector[i] = value;
        }
        return normalize(vector);
    }

    @Override
    public String modelId() {
        return MODEL_ID;
    }

    @Override
    public String modelRevision() {
        return REVISION;
    }

    private static float[] normalize(float[] vector) {
        double sum = 0;
        for (float value : vector) {
            sum += (double) value * value;
        }
        double norm = Math.sqrt(sum);
        if (norm == 0) {
            return vector;
        }
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (float) (vector[i] / norm);
        }
        return vector;
    }

    private static byte[] sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable.", impossible);
        }
    }
}
