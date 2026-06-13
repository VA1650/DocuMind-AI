package annie312.langchain;

import java.util.*;

public class TextChunker {

    private static final int MAX_CHARS = 1200;

    public static List<String> chunk(String text) {

        List<String> chunks = new ArrayList<>();

        // 1. сначала по абзацам
        String[] paragraphs = text.split("\\n\\n+");

        StringBuilder current = new StringBuilder();

        for (String p : paragraphs) {

            if (current.length() + p.length() > MAX_CHARS) {
                chunks.add(current.toString());
                current.setLength(0);
            }

            current.append(p).append("\n\n");
        }

        if (!current.isEmpty()) {
            chunks.add(current.toString());
        }

        return chunks;
    }
}