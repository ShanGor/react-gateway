package io.github.shangor.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = {"*"})
@Slf4j
public class OllamaProxyController {
    @Value("${ai.ollama.url}")
    String ollamaUrl;
    @Resource
    ObjectMapper objectMapper;

    /**
     * Will only return as text-stream.
     * @param requestText
     * @param request
     * @return
     */
    @PostMapping("/chat/ollama")
    public Flux<ServerSentEvent<OllamaChatCompletion>> chat(@RequestBody String requestText, ServerHttpRequest request) {
        Boolean stream;
        try {
            var m = objectMapper.readValue(requestText, Map.class);
            var streamInRequest = m.get("stream");
            if (streamInRequest != null && "false".equalsIgnoreCase(streamInRequest.toString())) {
                stream = false;
            } else {
                stream = true;
            }
        } catch (JsonProcessingException e) {
            return Flux.error(e);
        }

        WebClient.RequestBodySpec client;
        if (stream) {
            client = WebClient.create(ollamaUrl).post().header("Content-Type", "text/stream-event;charset=utf-8");
        } else {
            client = WebClient.create(ollamaUrl).post().header("Content-Type", "application/json;charset=utf-8");
        }
        for (var entry : request.getHeaders().entrySet()) {
            client = client.header(entry.getKey(), entry.getValue().get(0));
        }
        return client.bodyValue(requestText)
                .retrieve()
                .bodyToFlux(OllamaChatCompletion.class)
                .map(ollamaChatCompletion -> ServerSentEvent.builder(ollamaChatCompletion).build());
    }

    @Data
    public static class OllamaChatCompletion {
        private String model;
        private String created_at;
        private String response;
        private Boolean done;
        private List<Integer> context;
        private Long total_duration;
        private Long load_duration;
        private Long prompt_eval_count;
        private Long prompt_eval_duration;
        private Long eval_count;
        private Long eval_duration;
    }
}
