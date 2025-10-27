package io.github.shangor.data.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LlmOptions {
    private String model;
    private double temperature;
    private int maxCompletionTokens;
}
