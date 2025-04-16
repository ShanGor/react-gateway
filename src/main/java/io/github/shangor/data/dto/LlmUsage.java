package io.github.shangor.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.ai.chat.metadata.Usage;

@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmUsage implements Usage {
    private Integer promptTokens;
    private Integer completionTokens;
    private Object nativeUsage;
    @Override
    public Integer getPromptTokens() {
        return promptTokens;
    }

    @Override
    public Integer getCompletionTokens() {
        return completionTokens;
    }

    @Override
    public Object getNativeUsage() {
        return nativeUsage;
    }

    public static LlmUsage from(Usage usage) {
        return LlmUsage.builder()
                .promptTokens(usage.getPromptTokens())
                .completionTokens(usage.getCompletionTokens())
                .nativeUsage(usage.getNativeUsage())
                .build();
    }

}
