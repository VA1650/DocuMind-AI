package annie312.langchain;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final ChatLanguageModel chatLanguageModel;
    private final MessageWindowChatMemory chatMemory;
    private volatile String documentsContext = "";

    public AiController(ChatLanguageModel chatLanguageModel, MessageWindowChatMemory chatMemory) {
        this.chatLanguageModel = chatLanguageModel;
        this.chatMemory = chatMemory;
    }

    @GetMapping("/load-repo")
    public String loadRepo(@RequestParam("path") String repoPath) {
        try {
            String normalizedPath = repoPath.replace("\\", "/");
            Path start = Paths.get(normalizedPath);

            if (!Files.exists(start)) {
                return "Ошибка: Путь не существует!";
            }

            try (var stream = Files.walk(start)) {
                this.documentsContext = stream
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().toLowerCase().endsWith(".md") || p.toString().toLowerCase().endsWith(".txt"))
                        .map(p -> {
                            try {
                                return "\n\n[FILE: " + p.getFileName() + "]\n"
                                        + Files.readString(p)
                                        + "\n[END OF FILE]\n\n";
                            } catch (IOException e) {
                                return "";
                            }
                        })
                        .collect(Collectors.joining("\n"));
            }

            chatMemory.clear();

            if (documentsContext.trim().isEmpty()) {
                return "Файлы .md/.txt не найдены.";
            }

            return "Репозиторий загружен! Символов: " + documentsContext.length();
        } catch (Exception e) {
            return "Ошибка: " + e.getMessage();
        }
    }

    @GetMapping("/chat")
    public String chat(@RequestParam("question") String question) {
        if (question == null || question.trim().isEmpty()) {
            return "<div class='text-red-400 font-medium'>Вопрос пуст!</div>";
        }

        try {
            String systemPrompt =
                    "Ты — полезный технический ассистент. Перед тобой данные проекта.\n" +
                            "Изучи их и развернуто ответь на вопрос пользователя на русском языке.\n\n" +
                            "ДАННЫЕ ПРОЕКТА:\n" + this.documentsContext;

            Assistant assistant = AiServices.builder(Assistant.class)
                    .chatLanguageModel(chatLanguageModel)
                    .chatMemory(chatMemory)
                    .systemMessageProvider(chatId -> systemPrompt)
                    .build();

            String aiResponse = assistant.chat(question);

            return String.format(
                    "<div class='bg-gray-800 p-3 rounded border border-gray-700'>" +
                            "  <div class='text-emerald-400 font-bold mb-1'>Вы:</div> <div class='text-gray-200 mb-3'>%s</div>" +
                            "  <div class='text-blue-400 font-bold mb-1'>Llama 3:</div> <div class='text-gray-100 whitespace-pre-wrap'>%s</div>" +
                            "</div>",
                    question, aiResponse
            );

        } catch (Exception e) {
            return String.format("<div class='text-red-400 p-2'>Ошибка: %s</div>", e.getMessage());
        }
    }

    @GetMapping("/clear-memory")
    public String clearMemory() {
        chatMemory.clear();
        return "Память чата успешно очищена!";
    }
}