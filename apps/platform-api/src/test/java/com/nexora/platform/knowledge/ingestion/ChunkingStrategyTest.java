package com.nexora.platform.knowledge.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChunkingStrategyTest {

    private final ChunkingStrategy strategy = new ChunkingStrategy();

    @Test
    void chunkingIsDeterministicForIdenticalInput() {
        String input = "Paragraph one about publishing.\n\nParagraph two about rollback.\n\nParagraph three about themes.";
        List<ChunkingStrategy.Chunk> first = strategy.chunk(input);
        List<ChunkingStrategy.Chunk> second = strategy.chunk(input);
        assertThat(first).isEqualTo(second);
    }

    @Test
    void chunksSplitOnParagraphBoundariesWithinBudget() {
        StringBuilder input = new StringBuilder();
        for (int i = 0; i < 30; i++) {
            input.append("Paragraph number ").append(i).append(" begins with a sentence that contains enough words")
                    .append(" to measure the token budget accurately. It continues with additional sentences because")
                    .append(" each paragraph must exceed the ceiling by itself when many of them are combined.")
                    .append(" The publishing workflow freezes immutable versions and rollback creates a new version.")
                    .append(" Themes are versioned as well and their tokens serialize into bounded manifests.")
                    .append("\n\n");
        }
        List<ChunkingStrategy.Chunk> chunks = strategy.chunk(input.toString());
        assertThat(chunks).isNotEmpty();
        assertThat(chunks.size()).isGreaterThan(1);
        for (ChunkingStrategy.Chunk chunk : chunks) {
            assertThat(ChunkingStrategy.estimateTokens(chunk.text())).isLessThanOrEqualTo(1500);
        }
    }

    @Test
    void emptyAndBlankInputsProduceNoChunks() {
        assertThat(strategy.chunk("")).isEmpty();
        assertThat(strategy.chunk("   \n\n  ")).isEmpty();
    }

    @Test
    void everyChunkCarriesADeterministicSha256() {
        String input = "Single chunk document.";
        ChunkingStrategy.Chunk chunk = strategy.chunk(input).getFirst();
        assertThat(chunk.sha256()).matches("^[a-f0-9]{64}$");
        assertThat(chunk.sha256()).isEqualTo(ChunkingStrategy.Chunk.of(chunk.text()).sha256());
    }

    @Test
    void invalidBoundsAreRejected() {
        assertThatThrownBy(() -> new ChunkingStrategy(0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ChunkingStrategy(10, 10)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void longParagraphsSplitAtWordBoundaries() {
        String word = "antidisestablishmentarianism";
        StringBuilder longParagraph = new StringBuilder();
        while (longParagraph.length() < 20000) {
            longParagraph.append(word).append(' ');
        }
        List<ChunkingStrategy.Chunk> chunks = strategy.chunk(longParagraph.toString());
        assertThat(chunks.size()).isGreaterThan(1);
        for (ChunkingStrategy.Chunk chunk : chunks) {
            assertThat(ChunkingStrategy.estimateTokens(chunk.text())).isLessThanOrEqualTo(1500);
        }
    }

    @Test
    void chunkTextNeverExceedsHardCharacterLimit() {
        String word = "x";
        StringBuilder longParagraph = new StringBuilder();
        while (longParagraph.length() < 30000) {
            longParagraph.append(word);
        }
        List<ChunkingStrategy.Chunk> chunks = strategy.chunk(longParagraph.toString());
        for (ChunkingStrategy.Chunk chunk : chunks) {
            assertThat(chunk.text().length()).isLessThanOrEqualTo(1500 * 4 + 1);
        }
    }
}
