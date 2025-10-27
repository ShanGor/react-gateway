package io.github.shangor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.shangor.data.dto.LlmOptions;
import io.github.shangor.data.dto.OpenAiRequestMessage;
import io.github.shangor.exception.UserException;
import io.github.shangor.gateway.ModelController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class LlmService {

    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public LlmService(ObjectMapper objectMapper, @Qualifier("llmWebClient")WebClient.Builder webClientBuilder) {
        this.objectMapper = objectMapper;
        webClient = webClientBuilder.build();
    }

    public Flux<String> chatCompletion(LlmOptions options, List<OpenAiRequestMessage> messages, Map<String, String> headers) {
        var currentModel = ModelController.modelHub.get(options.getModel());
        if (currentModel == null) {
            log.error("Model not found: {}", options.getModel());
            return Flux.empty();
        }
        var requestBuilder = webClient.post().uri(currentModel.getUrl());
        if (headers != null) {
            for (var entry : headers.entrySet()) {
                requestBuilder = requestBuilder.header(entry.getKey(), entry.getValue());
            }
        }
        var template = currentModel.getTemplate();
        template.put("messages", messages);
        if (template.containsKey("model")) {
            template.put("model", options.getModel());
        }
        if (template.containsKey("temperature"))
            template.put("temperature", options.getTemperature());
        if (template.containsKey("max_tokens"))
            template.put("max_tokens", options.getMaxCompletionTokens());
        if (template.containsKey("max_completion_tokens"))
            template.put("max_completion_tokens", options.getMaxCompletionTokens());

        try {
            String body = objectMapper.writeValueAsString(template);
            return requestBuilder.bodyValue(body).retrieve()
                    .bodyToFlux(String.class);
        } catch (JsonProcessingException e) {
            throw new UserException(400, "E002", "Fail to compose LLM request for invalid JSON: " + e.getMessage());
        }

    }
}
