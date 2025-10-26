package io.github.shangor.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.openai.api.OpenAiApi;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private OpenAiApi.ChatCompletionRequest request;
    private ChatOptions options;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatOptions {
        private boolean useRag;
        private int ragTopK;
        private int includeHistoryCount;
    }
}
