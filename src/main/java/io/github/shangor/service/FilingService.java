package io.github.shangor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
@Slf4j
public class FilingService {
    /**
     * You can define as many parameters as you want.
     */
    @Tool(description = "save a text file to file system")
    public String saveTextToFile(String fileName, String text) {
        log.info("Tooling request: saveTextToFile: fileName={}, context={}", fileName, text);
        try {
            Files.writeString(Paths.get("c:\\tmp", fileName), text);
        } catch (IOException e) {
            log.error("Failed to save file: {}", e.getMessage());
            return "Failed to save file: " + e.getMessage();
        }
        log.info("saveTextToFile success!");
        return "saved successfully";
    }
}
