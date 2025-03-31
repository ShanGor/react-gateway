package io.github.shangor.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.shangor.llm.LlmCompletionFunc;
import io.github.shangor.llm.impl.OllamaCompletionFunc;
import io.github.shangor.llm.impl.OllamaEmbeddingFunc;
import io.github.shangor.llm.pojo.OpenAiCompletionRequest;
import io.github.shangor.llm.service.HttpService;
import io.github.shangor.util.GenUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.ai.document.Document;

@RestController
@CrossOrigin(origins = {"*"})
@Slf4j
public class OllamaProxyController {
    @Resource
    VectorStore vectorStore;

    private final ObjectMapper objectMapper;

    private final LlmCompletionFunc completionFunc;

    private final Map<String, Disposable> requestPool = new ConcurrentHashMap<>();

    private final String ollamaUrl;

    private static final ServerSentEvent<String> DONE = ServerSentEvent.builder("DONE").build();

    public OllamaProxyController(@Value("${ai.ollama.url}") String ollamaUrl,
                                 ObjectMapper objectMapper,
                                 HttpService httpService) {
        this.ollamaUrl = ollamaUrl;
        var ollamaCompletionFunc = new OllamaCompletionFunc();
        var ollamaEmbeddingFunc = new OllamaEmbeddingFunc();
        ollamaCompletionFunc.setUrl(URI.create("%s/api/chat".formatted(ollamaUrl)));
        ollamaCompletionFunc.setObjectMapper(objectMapper);
        ollamaCompletionFunc.setHttpService(httpService);
        ollamaEmbeddingFunc.setUrl(URI.create("%s/api/embed".formatted(ollamaUrl)));
        ollamaEmbeddingFunc.setHttpService(httpService);
        this.completionFunc = ollamaCompletionFunc;
        this.objectMapper = objectMapper;
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
    public Flux<Document> getEmbeddings(@RequestBody String body, @PathVariable int topK) {
        return Flux.create(sink -> Thread.ofVirtual().start(() -> {
            vectorStore.similaritySearch(SearchRequest.builder().topK(topK).query(body).build()).forEach(sink::next);
            sink.complete();
        }));
    }

    @GetMapping(value = "/api/tags", produces = "application/json")
    public Mono<String> listModels() {
        return WebClient.create(ollamaUrl).get().uri("/api/tags").retrieve().bodyToMono(String.class);
    }
}
