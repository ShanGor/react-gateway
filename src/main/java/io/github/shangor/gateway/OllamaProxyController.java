package io.github.shangor.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.shangor.llm.EmbeddingFunc;
import io.github.shangor.llm.LlmCompletionFunc;
import io.github.shangor.llm.impl.OllamaCompletionFunc;
import io.github.shangor.llm.impl.OllamaEmbeddingFunc;
import io.github.shangor.llm.pojo.OpenAiCompletionRequest;
import io.github.shangor.llm.service.HttpService;
import io.github.shangor.util.GenUtils;
import io.r2dbc.postgresql.codec.Json;
import io.r2dbc.postgresql.codec.Vector;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@CrossOrigin(origins = {"*"})
@Slf4j
public class OllamaProxyController {

    private final ObjectMapper objectMapper;

    private final LlmCompletionFunc completionFunc;

    private final Map<String, Disposable> requestPool = new ConcurrentHashMap<>();
    private final EmbeddingFunc embeddingFunc;

    private final String ollamaUrl;

    private static final ServerSentEvent<String> DONE = ServerSentEvent.builder("DONE").build();

    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    public OllamaProxyController(@Value("${ai.ollama.url}") String ollamaUrl,
                                 ObjectMapper objectMapper,
                                 HttpService httpService,
                                 R2dbcEntityTemplate r2dbcEntityTemplate) {
        this.ollamaUrl = ollamaUrl;
        var ollamaCompletionFunc = new OllamaCompletionFunc();
        var ollamaEmbeddingFunc = new OllamaEmbeddingFunc();
        ollamaCompletionFunc.setUrl(URI.create("%s/api/chat".formatted(ollamaUrl)));
        ollamaCompletionFunc.setObjectMapper(objectMapper);
        ollamaCompletionFunc.setHttpService(httpService);
        ollamaEmbeddingFunc.setUrl(URI.create("%s/api/embed".formatted(ollamaUrl)));
        ollamaEmbeddingFunc.setHttpService(httpService);
        this.completionFunc = ollamaCompletionFunc;
        this.r2dbcEntityTemplate = r2dbcEntityTemplate;
        this.objectMapper = objectMapper;
        this.embeddingFunc = ollamaEmbeddingFunc;
    }

    /**
     * Will only return as text-stream.
     */
    @PostMapping("/ollama/chat")
    public Flux<ServerSentEvent<String>> chat(@RequestBody String requestText) {
        LlmCompletionFunc.Options options = new LlmCompletionFunc.Options();
        options.setStream(true);
        List<LlmCompletionFunc.CompletionMessage> messages;
        try {
            var request = objectMapper.readValue(requestText, OpenAiCompletionRequest.class);
            options.setModel(request.getModel());
            var ollamaRequest = OllamaCompletionFunc.OllamaRequest.fromOpenAiRequest(request);
            messages = ollamaRequest.getMessages();
        } catch (JsonProcessingException e) {
            return Flux.error(e);
        }

        var requestId = UUID.randomUUID().toString();

        var cancelDisposable = Schedulers.newSingle(requestId);
        requestPool.put(requestId, cancelDisposable);
        return completionFunc.completeStream(messages, options)
                .cancelOn(cancelDisposable)
                .doFinally(signal -> clearRequest(requestId))
                .onErrorStop()
                .mapNotNull(sse -> {
                    var o = sse.data();
                    if (o != null) o.setId(requestId);
                    if (requestPool.containsKey(requestId))
                        return ServerSentEvent.builder(GenUtils.objectToJsonSnake(o)).id(requestId).build();
                    else
                        return null;
                })
                .concatWith(Flux.just(DONE));

    }

    private void clearRequest(String requestId) {
        requestPool.computeIfPresent(requestId, (k, v) -> {
            try {
                requestPool.remove(requestId);
                v.dispose();
                log.info("Successfully cleared request {}", requestId);
            } catch (Exception ignored) { }
            return null;
        });

    }

    @GetMapping(value = "/api/cancel/{requestId}")
    public String cancel(@PathVariable String requestId) {
        clearRequest(requestId);
        return "ok";
    }

    @PostMapping("/api/find-embeddings/{topK}")
    public Flux getEmbeddings(@RequestBody String body, @PathVariable int topK) {
        var embedding = embeddingFunc.convert(body, "all-minilm");

        var sql = "select (embedding <-> :eb) as distance, * from knowledge_base ORDER BY distance limit :tk";

        return r2dbcEntityTemplate.getDatabaseClient().sql(sql).bind(0, embedding).bind(1, topK).fetch()
                .all()
                .map(o -> {
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
