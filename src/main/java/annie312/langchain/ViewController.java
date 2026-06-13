package annie312.langchain; // Твой пакет

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/")
    public String index() {
        return "index"; // Откроет файл index.html из папки templates
    }
}