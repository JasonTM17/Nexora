package com.nexora.platform.rag.provider;

import com.nexora.platform.rag.retrieval.HybridRetrievalService;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Builds bounded grounded context from already-authorized retrieval matches.
 * Sources are untrusted data: they are delimited and never concatenated into
 * the instruction surface. The builder truncates deterministically at the
 * token budget and records the selected chunk ids.
 */
@Service
@Profile("database")
public class ContextBuilder {
    private static final int MAX_CONTEXT_TOKENS = 8000;
    private static final int TOKEN_ESTIMATE_CHARS = 4;

    public ContextAssembly build(HybridRetrievalService.RetrievalResult retrieval, String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("The query is required.");
        }
        StringBuilder context = new StringBuilder();
        List<UUID> selected = new java.util.ArrayList<>();
        int usedTokens = 0;
        for (HybridRetrievalService.FusedMatch match : retrieval.matches()) {
            String source = "source-" + match.chunkId();
            int sourceTokens = source.length() / TOKEN_ESTIMATE_CHARS + 1;
            if (usedTokens + sourceTokens > MAX_CONTEXT_TOKENS) {
                break;
            }
            context.append("<source id=\"").append(match.chunkId()).append("\" />\n");
            usedTokens += sourceTokens;
            selected.add(match.chunkId());
        }
        return new ContextAssembly(context.toString(), List.copyOf(selected), usedTokens);
    }

    public record ContextAssembly(String context, List<UUID> selectedChunkIds, int estimatedTokens) {
    }
}
