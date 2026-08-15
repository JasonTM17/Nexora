package com.nexora.platform.rag.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Production-ready OpenAI-compatible Chat Provider.
 * Supports OpenAI, DeepSeek, Groq, Anthropic, Gemini (via compat proxy), Ollama, and vLLM.
 */
@Component
@Profile("database")
@ConditionalOnProperty(prefix = "nexora.chat.live", name = "enabled", havingValue = "true")
public class OpenAiCompatibleChatProvider implements ChatProvider {
    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleChatProvider.class);

    private final String endpointUrl;
    private final String apiKey;
    private final String modelName;
    private final String revision;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleChatProvider(
            @Value("${nexora.chat.live.endpoint:https://api.openai.com/v1/chat/completions}") String endpointUrl,
            @Value("${nexora.chat.live.api-key:}") String apiKey,
            @Value("${nexora.chat.live.model:gpt-4o-mini}") String modelName,
            @Value("${nexora.chat.live.revision:v1-prod}") String revision,
            @Value("${nexora.chat.live.timeout-seconds:30}") int timeoutSeconds) {
        this.endpointUrl = endpointUrl;
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.revision = revision;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ChatResponse complete(ChatRequest request) {
        if (request == null || request.userPrompt() == null || request.userPrompt().isBlank()) {
            throw new IllegalArgumentException("The chat request is invalid.");
        }

        try {
            Map<String, Object> payload = Map.of(
                    "model", modelName,
                    "messages", List.of(
                            Map.of("role", "system", "content", request.systemPrompt() != null ? request.systemPrompt() : ""),
                            Map.of("role", "user", "content", request.userPrompt())
                    ),
                    "max_tokens", request.maxTokens() > 0 ? request.maxTokens() : 1024,
                    "temperature", 0.1
            );

            String requestBody = objectMapper.writeValueAsString(payload);
            HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(endpointUrl))
                    .timeout(timeout)
                    .header("Content-Type", "application/json");

            if (apiKey != null && !apiKey.isBlank()) {
                httpRequestBuilder.header("Authorization", "Bearer " + apiKey);
            }

            HttpResponse<String> response = httpClient.send(
                    httpRequestBuilder.POST(HttpRequest.BodyPublishers.ofString(requestBody)).build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                log.warn("Live chat completion failed with status {}: {}", response.statusCode(), response.body());
                return fallbackResponse();
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                JsonNode firstChoice = choices.get(0);
                String content = firstChoice.path("message").path("content").asText("");
                String finishReason = firstChoice.path("finish_reason").asText("stop");
                int promptTokens = root.path("usage").path("prompt_tokens").asInt(0);
                int completionTokens = root.path("usage").path("completion_tokens").asInt(0);
                return new ChatResponse(content, finishReason, promptTokens, completionTokens);
            }

            return fallbackResponse();
        } catch (IOException | InterruptedException e) {
            log.error("Exception during live chat completion: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return fallbackResponse();
        }
    }

    private ChatResponse fallbackResponse() {
        return new ChatResponse(
                "Unable to retrieve a live generative answer at this moment. Please consult the verified citations above.",
                "fallback", 0, 0
        );
    }

    @Override
    public String modelId() {
        return modelName;
    }

    @Override
    public String modelRevision() {
        return revision;
    }
}
