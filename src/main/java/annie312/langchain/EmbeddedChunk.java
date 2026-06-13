package annie312.langchain;

public record EmbeddedChunk(
        DocumentChunk chunk,
        float[] vector
) {}