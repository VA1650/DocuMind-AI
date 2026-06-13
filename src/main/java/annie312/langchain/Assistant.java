package annie312.langchain;

import dev.langchain4j.service.UserMessage;

public interface Assistant {

    // Никаких @SystemMessage здесь! Всё управление идет через контроллер
    String chat(@UserMessage String question);
}