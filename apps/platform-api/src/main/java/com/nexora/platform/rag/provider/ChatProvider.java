package com.nexora.platform.rag.provider;

/**
 * Bounded chat provider abstraction. Providers receive only already-authorized
 * minimal context; implementations must treat model output as untrusted data.
 */
public interface ChatProvider {
    String MODEL_ID = "deepseek-v4-flash";

    ChatResponse complete(ChatRequest request);

    String modelId();

    String modelRevision();

    record ChatRequest(String systemPrompt, String userPrompt, int maxTokens) {
    }

    record ChatResponse(String content, String finishReason, int promptTokens, int completionTokens) {
    }
}
