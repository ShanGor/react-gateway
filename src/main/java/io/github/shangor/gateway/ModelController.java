package io.github.shangor.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.shangor.data.dto.ModelsConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
public class ModelController {
    public static final Map<String, ModelsConfig.Model> modelHub = new HashMap<>();
    private final List<ModelsConfig.Model> models;
    public ModelController(ObjectMapper objectMapper) {
        models = new LinkedList<>();
        var usingOllama = "true".equals(System.getenv("USING_OLLAMA"));

        if (usingOllama) {
            log.info("Using Ollama enabled, trying to load models config");
            try(var ins = ClassLoader.getSystemResource("models/ollama.json").openStream()) {
                var modelsConfig = objectMapper.readValue(ins, ModelsConfig.class);
                modelsConfig.getModels().forEach(model -> {
                    if ("llm".equals(model.getType())) {
                        model.setTemplate(modelsConfig.getLlmTemplate());
                    } else if ("embedding".equals(model.getType())) {
                        model.setTemplate(modelsConfig.getEmbeddingTemplate());
                    }
                    models.add(model);
                    modelHub.put(model.getName(), model);
                });
            } catch (IOException e) {
                log.error("Failed to load models config", e);
            }
        }
    }
    @GetMapping(value = "/api/models", produces = "application/json")
    public List<ModelsConfig.Model> listModels() {
        return models;
    }
}
