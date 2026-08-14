package com.nexora.platform.rag.provider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Recorded deterministic chat provider for CI. It never claims to be live
 * model output and returns a fixed grounded answer for the accepted fixtures.
 */
@Component
@Profile("database")
@ConditionalOnProperty(
        prefix = "nexora.chat.deepseek",
        name = "enabled",
        havingValue = "false",
        matchIfMissing = true)
public class DeterministicChatProvider implements ChatProvider {
    private static final String REVISION = "deterministic-ci-v1";

    @Override
    public ChatResponse complete(ChatRequest request) {
        if (request == null || request.userPrompt() == null || request.userPrompt().isBlank()) {
            throw new IllegalArgumentException("The chat request is invalid.");
        }
        return new ChatResponse(
                "Publishing creates a new immutable version; rollback is a new version referencing the prior one.",
                "stop", 12, 18);
    }

    @Override
    public String modelId() {
        return MODEL_ID;
    }

    @Override
    public String modelRevision() {
        return REVISION;
    }
}
