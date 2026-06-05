package annie312.langchain;

import dev.langchain4j.service.UserMessage;

public interface Assistant {
    String chat(@UserMessage String question);
}