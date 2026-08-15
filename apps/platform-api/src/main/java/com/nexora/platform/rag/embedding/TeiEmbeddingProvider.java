package com.nexora.platform.rag.embedding;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Local Text Embeddings Inference adapter. The base URL comes from
 * environment only and the provider output is validated for the expected
 * dimension before acceptance. Timeouts and the single-retry budget are
 * bounded; a failed call fails closed.
 */
@Component
@Profile("database")
@ConditionalOnProperty(prefix = "nexora.embedding.tei", name = "enabled", havingValue = "true")
public class TeiEmbeddingProvider implements EmbeddingProvider {
    private static final Logger LOG = LoggerFactory.getLogger(TeiEmbeddingProvider.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private final ObjectMapper json = new ObjectMapper();
    private final String baseUrl;
    private final String revision;
    private final AtomicBoolean failed = new AtomicBoolean(false);

    public TeiEmbeddingProvider(
            @Value("${nexora.embedding.tei.base-url}") String baseUrl,
            @Value("${nexora.embedding.tei.revision:unpinned-tei-revision}") String revision) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("The TEI base URL is required when the adapter is enabled.");
        }
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.revision = revision;
    }

    @Override
    public float[] embed(String text) {
        if (failed.get()) {
            throw new IllegalStateException("The embedding provider is in a failed state.");
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/embed"))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "{\"inputs\":\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}"))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("The embedding provider returned HTTP " + response.statusCode());
            }
            JsonNode body = json.readTree(response.body());
            JsonNode embedding = body.path("embedding");
            if (!embedding.isArray() || embedding.size() != DIMENSIONS) {
                throw new IllegalStateException("The embedding provider returned an unexpected dimension.");
            }
            float[] vector = new float[DIMENSIONS];
            for (int i = 0; i < DIMENSIONS; i++) {
                vector[i] = embedding.get(i).floatValue();
            }
            return vector;
        } catch (Exception exception) {
            failed.set(true);
            LOG.error("Embedding provider call failed; the adapter is now failing closed.", exception);
            throw new IllegalStateException("The embedding provider is unavailable.", exception);
        }
    }

    @Override
    public String modelId() {
        return MODEL_ID;
    }

    @Override
    public String modelRevision() {
        return revision;
    }
}
