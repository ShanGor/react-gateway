package io.github.shangor.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private String conversationId;
    /**
     * Default is null
     */
    private String agentId;
    private String model;
    private String prompt;
    private List<String> referenceDocuments;
    private List<String> mediaDataUrls;
    private double temperature = 0.7;
    private int maxCompletionTokens = 512;
}
