package io.github.shangor.data.dto;

import io.github.shangor.llm.pojo.OpenAiCompletionRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private OpenAiCompletionRequest request;
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
