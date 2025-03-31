package io.github.shangor.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.shangor.llm.LlmCompletionFunc;
import io.github.shangor.llm.impl.OllamaCompletionFunc;
import io.github.shangor.llm.impl.OllamaEmbeddingFunc;
import io.github.shangor.llm.pojo.OpenAiCompletionRequest;
import io.github.shangor.llm.pojo.OpenAiLlmStreamResult;
import io.github.shangor.llm.service.HttpService;
import io.github.shangor.util.DateTimeUtils;
import io.github.shangor.util.GenUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
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

    private final ChatClient.Builder chatClientBuilder;

    private static final ServerSentEvent<String> DONE = ServerSentEvent.builder("DONE").build();

    public OllamaProxyController(@Value("${ai.ollama.url}") String ollamaUrl,
                                 ObjectMapper objectMapper,
                                 ChatClient.Builder chatClientBuilder,
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
        this.chatClientBuilder = chatClientBuilder;
    }

    /**
     * Will only return as text-stream.
     */
    @PostMapping("/ollama/chat-rag")
    public Flux<ServerSentEvent<String>> chatWithRag(@RequestBody String requestText) {
        LlmCompletionFunc.Options options = new LlmCompletionFunc.Options();
        options.setStream(true);
        List<org.springframework.ai.chat.messages.Message> messages = new LinkedList<>();
        try {
            var request = objectMapper.readValue(requestText, OpenAiCompletionRequest.class);
            options.setModel(request.getModel());
            var ollamaRequest = OllamaCompletionFunc.OllamaRequest.fromOpenAiRequest(request);
            ollamaRequest.getMessages().forEach(msg -> {
                switch (msg.getRole()) {
                    case "system" -> messages.add(new SystemMessage(msg.getContent()));
                    case "user" -> messages.add(new UserMessage(msg.getContent()));
                    case "assistant" -> messages.add(new AssistantMessage(msg.getContent()));
                    default -> throw new IllegalArgumentException("Unsupported role: " + msg.getRole());
                }
            });
        } catch (JsonProcessingException e) {
            return Flux.error(e);
        }

        var requestId = UUID.randomUUID().toString();

        var cancelDisposable = Schedulers.newSingle(requestId);
        requestPool.put(requestId, cancelDisposable);
        return chatClientBuilder.defaultOptions(ChatOptions.builder().model(options.getModel()).build()).build()
                .prompt()
                .advisors(new QuestionAnswerAdvisor(vectorStore, SearchRequest.builder().topK(5).build()))
                .messages(messages).stream().chatResponse()
                .cancelOn(cancelDisposable)
                .doFinally(signal -> clearRequest(requestId))
                .onErrorStop()
                .mapNotNull(o -> {
                    if (o == null) return null;
                    if (requestPool.containsKey(requestId))
                        return ServerSentEvent.builder(GenUtils.objectToJsonSnake(toOpenAiLlmStreamResult(o, requestId))).id(requestId).event("spring-ai-llm").build();
                    else
                        return null;
                })
                .concatWith(Flux.just(DONE));
    }

    public static OpenAiLlmStreamResult toOpenAiLlmStreamResult(ChatResponse obj, String requestId) {
        var res = new OpenAiLlmStreamResult();
        res.setId(requestId);
        res.setModel(obj.getMetadata().getModel());
        res.setCreated(System.currentTimeMillis());
        String finishReason = null;
        if (obj.getResult().getMetadata().containsKey("finish_reason")) {
            finishReason = obj.getResult().getMetadata().getFinishReason();
            var usage = new OpenAiLlmStreamResult.Usage();
            usage.setTotal_tokens(obj.getMetadata().getUsage().getTotalTokens());
            usage.setPrompt_tokens(obj.getMetadata().getUsage().getPromptTokens());
            usage.setCompletion_tokens(obj.getMetadata().getUsage().getCompletionTokens());
            res.setUsage(usage);
        }
        var choices = new LinkedList<OpenAiLlmStreamResult.Choice>();
        var idx = 0;
        for (var result : obj.getResults()) {
            var choice = new OpenAiLlmStreamResult.Choice();
            var msg = new LlmCompletionFunc.CompletionMessage();
            msg.setRole("assistant");
            msg.setContent(result.getOutput().getText());
            var medias = result.getOutput().getMedia();
            if (medias != null && !medias.isEmpty()) {
                //TODO
                msg.setImages(medias.stream().map(media -> Base64.getEncoder().encodeToString(media.getDataAsByteArray())).toList());
            }
            choice.setIndex(idx);
            choice.setDelta(msg);
            choice.setFinish_reason(finishReason);
            choices.add(choice);
            idx++;
        }
        res.setChoices(choices);

        return res;
    }

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
