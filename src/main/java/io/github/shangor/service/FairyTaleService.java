package io.github.shangor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FairyTaleService {
    /**
     * You can define as many parameters as you want.
     */
    @Tool(description = "Generate a fairy tale")
    public String tellStory(String author) {
        log.info("Tooling request: tell story with author: {}", author);
        return "%s: Once upon a time, there is a little girl, she sells matches for living. one day.. she died..".formatted(author);
    }
}
