package io.github.shangor.data.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ModelsConfig {
    private List<Model> models;
    @Data
    public static class Model {
        private String name;
        private String type;
        private String description;
        private String url;
        private Map<String,  Object> template;
    }

    @JsonAlias("llm-template")
    private Map<String, Object> llmTemplate;
    @JsonAlias("embedding-template")
    private Map<String, Object> embeddingTemplate;
}
