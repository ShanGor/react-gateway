package io.github.shangor.data.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.shangor.statemachine.util.JsonUtil;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Data
@Slf4j
public class LlmContext {
    private String inputText;
    private List<String> images;
    private List<String> textDocs;
    private LlmUsage usage;

    private List<Message> chatHistory;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Message {
        private String role;
        private String text;
        private List<String> images;
    }

    public static LlmContext from(String mapStr) {
        try {
            return JsonUtil.getObjectMapper().readValue(mapStr, LlmContext.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse the LlmContext: {}", mapStr);
            e.printStackTrace();
            return null;
        }
    }


    public String toString() {
        try {
            return JsonUtil.getObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse the LlmUsage: {}", this);
            e.printStackTrace();
            return null;
        }
    }


}
