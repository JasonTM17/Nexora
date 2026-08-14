package com.nexora.platform.knowledge.ingestion;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Deterministic bounded-overlap chunking for the nexora-chunk-v1 strategy.
 * Chunks split on paragraph boundaries first, then on hard token-budget
 * overflow, and never exceed the configured token ceiling. The strategy
 * version is recorded on every chunk so a later strategy change forces a
 * full reindex instead of mutating history.
 */
@Component
@Profile("database")
public final class ChunkingStrategy {
    public static final String STRATEGY_VERSION = "nexora-chunk-v1";
    private static final int DEFAULT_MAX_TOKENS = 1500;
    private static final int DEFAULT_OVERLAP_TOKENS = 100;
    private static final int TOKEN_ESTIMATE_CHARS = 4;

    private final int maxTokens;
    private final int overlapTokens;

    public ChunkingStrategy() {
        this(DEFAULT_MAX_TOKENS, DEFAULT_OVERLAP_TOKENS);
    }

    public ChunkingStrategy(int maxTokens, int overlapTokens) {
        if (maxTokens < 1 || overlapTokens < 0 || overlapTokens >= maxTokens) {
            throw new IllegalArgumentException("Invalid chunking bounds.");
        }
        this.maxTokens = maxTokens;
        this.overlapTokens = overlapTokens;
    }

    public List<Chunk> chunk(String normalizedText) {
        if (normalizedText == null) {
            throw new IllegalArgumentException("Chunk input text is required.");
        }
        String trimmed = normalizedText.trim();
        if (trimmed.isEmpty()) {
            return List.of();
        }
        List<String> paragraphs = splitParagraphs(trimmed);
        List<Chunk> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int currentTokens = 0;
        for (String paragraph : paragraphs) {
            int paragraphTokens = estimateTokens(paragraph);
            if (paragraphTokens > maxTokens) {
                flush(chunks, current);
                current = new StringBuilder();
                currentTokens = 0;
                splitLongParagraph(chunks, paragraph);
                continue;
            }
            if (currentTokens + paragraphTokens > maxTokens && currentTokens > 0) {
                flush(chunks, current);
                current = new StringBuilder();
                currentTokens = 0;
                appendOverlap(current, chunks);
            }
            appendParagraph(current, paragraph);
            currentTokens = estimateTokens(current.toString());
        }
        flush(chunks, current);
        return List.copyOf(chunks);
    }

    private void splitLongParagraph(List<Chunk> chunks, String paragraph) {
        int hardLimitChars = maxTokens * TOKEN_ESTIMATE_CHARS;
        int start = 0;
        while (start < paragraph.length()) {
            int end = Math.min(paragraph.length(), start + hardLimitChars);
            if (end < paragraph.length()) {
                int boundary = paragraph.lastIndexOf(' ', end);
                if (boundary > start + hardLimitChars / 2) {
                    end = boundary;
                }
            }
            chunks.add(Chunk.of(paragraph.substring(start, end).trim()));
            start = end;
        }
    }

    private void appendOverlap(StringBuilder target, List<Chunk> chunks) {
        if (chunks.isEmpty() || overlapTokens == 0) {
            return;
        }
        String previous = chunks.getLast().text();
        int overlapChars = Math.min(previous.length(), overlapTokens * TOKEN_ESTIMATE_CHARS);
        int boundary = previous.lastIndexOf(' ', previous.length() - overlapChars);
        if (boundary > 0) {
            target.append(previous, boundary + 1, previous.length()).append(' ');
        }
    }

    private void appendParagraph(StringBuilder target, String paragraph) {
        if (!target.isEmpty() && target.charAt(target.length() - 1) != ' ') {
            target.append(' ');
        }
        target.append(paragraph);
    }

    private void flush(List<Chunk> chunks, StringBuilder current) {
        if (!current.isEmpty()) {
            String text = current.toString().trim();
            if (!text.isEmpty()) {
                chunks.add(Chunk.of(text));
            }
            current.setLength(0);
        }
    }

    private static List<String> splitParagraphs(String text) {
        return List.of(text.split("\\n\\s*\\n"));
    }

    static int estimateTokens(String text) {
        int tokens = text.length() / TOKEN_ESTIMATE_CHARS;
        return tokens == 0 ? 1 : tokens;
    }

    public record Chunk(String text, String sha256) {
        public static Chunk of(String text) {
            return new Chunk(text, sha256(text));
        }

        private static String sha256(String text) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
            } catch (NoSuchAlgorithmException impossible) {
                throw new IllegalStateException("SHA-256 is unavailable.", impossible);
            }
        }
    }
}
