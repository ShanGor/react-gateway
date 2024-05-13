package io.github.shangor.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@CrossOrigin(origins = {"*"})
@Slf4j
public class OllamaProxyController {
    @Value("${ai.ollama.url}")
    String ollamaUrl;
    @Resource
    ObjectMapper objectMapper;

    private Map<String, Disposable> requestPool = new ConcurrentHashMap<>();

    /**
     * Will only return as text-stream.
     * @param requestText
     * @param request
     * @return
     */
    @PostMapping("/chat/ollama")
    public Flux<ServerSentEvent<String>> chat(@RequestBody String requestText, ServerHttpRequest request) {

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
            client = WebClient.create(ollamaUrl).post().uri("/api/chat").header("Content-Type", "text/stream-event;charset=utf-8");
        } else {
            client = WebClient.create(ollamaUrl).post().uri("/api/chat").header("Content-Type", "application/json;charset=utf-8");
        }
        for (var entry : request.getHeaders().entrySet()) {
            client = client.header(entry.getKey(), entry.getValue().get(0));
        }

        var requestId = UUID.randomUUID().toString();
        var startInfo = """
                {"id":"%s","done":false}""".formatted(requestId);
        var endInfo = """
                {"id":"%s","done":true}""".formatted(requestId);

        var cancelDisposable = Schedulers.newSingle(requestId);
        requestPool.put(requestId, cancelDisposable);
        return Flux.just(startInfo)
                .concatWith(client.bodyValue(requestText).retrieve().bodyToFlux(String.class))
                .cancelOn(cancelDisposable)
                .doFinally(signal -> clearRequest(requestId))
                .onErrorStop()
                .map(ollamaChatCompletion -> {
                    if (requestPool.containsKey(requestId))
                        return ServerSentEvent.builder(ollamaChatCompletion).build();
                    else
                        return ServerSentEvent.builder(endInfo).build();
                });

    }


    private void clearRequest(String requestId) {
        requestPool.computeIfPresent(requestId, (k, v) -> {
            try {
                var disposable = v;
                if (disposable != null) {
                    requestPool.remove(requestId);
                    disposable.dispose();
                    log.info("Successfully cleared request {}", requestId);
                }
            } catch (Exception e) { }
            return null;
        });

    }

    @GetMapping(value = "/api/cancel/{requestId}")
    public String cancel(@PathVariable String requestId) {
        clearRequest(requestId);
        return "ok";
    }

    @GetMapping(value = "/api/tags", produces = "application/json")
    public Mono<String> listModels() {
        return WebClient.create(ollamaUrl).get().uri("/api/tags").retrieve().bodyToMono(String.class);
    }
}
