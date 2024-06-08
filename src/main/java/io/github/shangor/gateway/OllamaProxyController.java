package io.github.shangor.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import io.r2dbc.postgresql.codec.Vector;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.List;
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

    @Resource
    R2dbcEntityTemplate r2dbcEntityTemplate;

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

    @PostMapping("/api/get-embeddings")
    public Flux getEmbeddings(@RequestBody String body) {
        return WebClient.create(ollamaUrl).post().uri("/api/embeddings").bodyValue(Map.of("model", "all-minilm", "prompt", body))
                .retrieve().bodyToMono(OllamaEmbedding.class).flatMapMany(ollamaEmbedding -> {
            var sql = "select * from knowledge_base ORDER BY  embedding <-> :eb limit 5";

            var embedding = Vector.of(ollamaEmbedding.getEmbedding());
            return r2dbcEntityTemplate.getDatabaseClient().sql(sql).bind(0, embedding).fetch().all().map(o -> {
                var map = new HashMap<String, Object>();
                o.forEach((k,v) -> {
                    if (v instanceof Json j) {
                        try {
                            map.put(k, objectMapper.readValue(j.asString(), Map.class));
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    } else if (!(v instanceof Vector)) {
                        map.put(k, v);
                    }
                });
                return map;
            });

        });
    }

    @Data
    public static class OllamaEmbedding {
        private List<Double> embedding;
    }

    @GetMapping(value = "/api/tags", produces = "application/json")
    public Mono<String> listModels() {
        return WebClient.create(ollamaUrl).get().uri("/api/tags").retrieve().bodyToMono(String.class);
    }
}
