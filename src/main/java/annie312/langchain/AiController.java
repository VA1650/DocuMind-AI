package annie312.langchain;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final ChatLanguageModel chatModel;
    private final EmbeddingModel embeddingModel;

    private volatile List<EmbeddedChunk> index = new ArrayList<>();

    private static final int BATCH_SIZE = 16;
    private static final double MIN_SCORE = 0.25;

    public AiController(ChatLanguageModel chatModel,
                        EmbeddingModel embeddingModel) {
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
    }

    @GetMapping("/load-repo")
    public String loadRepo(@RequestParam("path") String path) {

        System.out.println("=== LOAD REPO ===");

        try {
            Path root = Paths.get(path);

            if (!Files.exists(root)) {
                return "Путь не существует";
            }

            List<DocumentChunk> docs = new ArrayList<>();
            List<String> texts = new ArrayList<>();

            try (var stream = Files.walk(root)) {

                stream
                        .filter(Files::isRegularFile)
                        .filter(p -> {
                            String n = p.toString().toLowerCase();
                            return n.endsWith(".md")
                                    || n.endsWith(".txt")
                                    || n.endsWith(".html")
                                    || n.endsWith(".json")
                                    || n.endsWith(".yaml")
                                    || n.endsWith(".yml");
                        })
                        .forEach(p -> {
                            try {

                                String content = Files.readString(p);

                                List<String> chunks = TextChunker.chunk(content);

                                for (String c : chunks) {
                                    texts.add(c);
                                    docs.add(new DocumentChunk(c, p.getFileName().toString()));
                                }

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
            }

            List<float[]> vectors = embedBatch(texts);

            List<EmbeddedChunk> tempIndex = new ArrayList<>();

            for (int i = 0; i < docs.size(); i++) {
                tempIndex.add(new EmbeddedChunk(docs.get(i), vectors.get(i)));
            }

            this.index = tempIndex;

            System.out.println("=== INDEX SIZE === " + index.size());

            return "OK. Чанков: " + index.size();

        } catch (Exception e) {
            e.printStackTrace();
            return "Ошибка: " + e.getMessage();
        }
    }

    // =========================
    // BATCH EMBEDDINGS
    // =========================
    private List<float[]> embedBatch(List<String> texts) {

        List<float[]> result = new ArrayList<>();

        for (int i = 0; i < texts.size(); i += BATCH_SIZE) {

            int end = Math.min(i + BATCH_SIZE, texts.size());

            List<String> batch = texts.subList(i, end);

            for (String t : batch) {
                result.add(
                        embeddingModel.embed(t)
                                .content()
                                .vector()
                );
            }
        }

        return result;
    }

    // =========================
    // COSINE
    // =========================
    private double cosine(float[] a, float[] b) {

        double dot = 0;
        double na = 0;
        double nb = 0;

        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }

        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    // =========================
    // RETRIEVAL
    // =========================
    private String retrieve(String question) {

        float[] qv = embeddingModel
                .embed(question)
                .content()
                .vector();

        return index.stream()
                .map(e -> {
                    double score = cosine(qv, e.vector());
                    return new AbstractMap.SimpleEntry<>(e, score);
                })
                .filter(e -> e.getValue() > MIN_SCORE)
                .sorted((a, b) ->
                        Double.compare(b.getValue(), a.getValue())
                )
                .limit(5)
                .map(e ->
                        "[score=" + String.format("%.2f", e.getValue()) + "]\n" +
                                "[FILE: " + e.getKey().chunk().source() + "]\n" +
                                e.getKey().chunk().text()
                )
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    // =========================
    // CHAT
    // =========================
    @GetMapping("/chat")
    public String chat(@RequestParam("question") String question) {

        if (question == null || question.isBlank()) {
            return "empty";
        }

        String context = retrieve(question);

        String prompt =
                "Ты ассистент по документации.\n" +
                        "Отвечай ТОЛЬКО по контексту.\n\n" +
                        "КОНТЕКСТ:\n" +
                        context +
                        "\n\nВОПРОС:\n" +
                        question;

        String answer = chatModel.generate(prompt);

        return """
                <div class='bg-gray-800 p-3 rounded border border-gray-700'>
                    <div class='text-emerald-400 font-bold'>Вы:</div>
                    <div class='mb-2'>%s</div>

                    <div class='text-blue-400 font-bold'>AI:</div>
                    <div class='whitespace-pre-wrap'>%s</div>
                </div>
                """.formatted(question, answer);
    }

    // =========================
    // CLEAR
    // =========================
    @GetMapping("/clear")
    public String clear() {
        index = new ArrayList<>();
        return "cleared";
    }
}