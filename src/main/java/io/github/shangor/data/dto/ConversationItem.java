package io.github.shangor.data.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
public class ConversationItem {
    private String conversationId;
    private String id;
    private String model;
    private int tokens = -1;
    private String role;
    private String content;
    private List<String> referenceDocuments;
    private List<String> mediaDataUrls;

    @Data
    @Builder
    public static class Meta {
        private String model;
        private int tokens;
        private List<String> referenceDocuments;
    }
}
