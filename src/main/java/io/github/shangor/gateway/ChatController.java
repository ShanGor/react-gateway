package io.github.shangor.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.shangor.data.dto.ChatRequest;
import io.github.shangor.llm.LlmCompletionFunc;
import io.github.shangor.llm.pojo.OpenAiLlmStreamResult;
import io.github.shangor.llm.service.HttpService;
import io.github.shangor.util.GenUtils;
import io.github.shangor.util.IntegrationUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.ai.document.Document;

@RestController
@Slf4j
public class ChatController {
    @Resource
    VectorStore vectorStore;

    private final ObjectMapper objectMapper;

    private final Map<String, Disposable> requestPool = new ConcurrentHashMap<>();


    private final ChatClient.Builder chatClientBuilder;

    private static final ServerSentEvent<String> DONE = ServerSentEvent.builder("DONE").build();

    public ChatController(ObjectMapper objectMapper,
                          ChatClient.Builder chatClientBuilder,
                          HttpService httpService) {
        this.objectMapper = objectMapper;
        this.chatClientBuilder = chatClientBuilder;
    }

    /**
     * Will only return as text-stream.
     */
//    @PostMapping("/api/chat-rag")
//    public Flux<ServerSentEvent<String>> chatWithRag(@RequestBody String requestText) {
//        LlmCompletionFunc.Options options = new LlmCompletionFunc.Options();
//        options.setStream(true);
//        List<org.springframework.ai.chat.messages.Message> messages = new LinkedList<>();
//        ChatRequest chatRequest;
//        try {
//            chatRequest = objectMapper.readValue(requestText, ChatRequest.class);
//            var request = chatRequest.getRequest();
//            options.setModel(request.getModel());
//            var ollamaRequest = OllamaCompletionFunc.OllamaRequest.fromOpenAiRequest(request);
//            ollamaRequest.getMessages().forEach(msg -> {
//                switch (msg.getRole()) {
//                    case "system" -> messages.add(new SystemMessage(msg.getContent()));
//                    case "user" -> messages.add(new UserMessage(msg.getContent()));
//                    case "assistant" -> messages.add(new AssistantMessage(msg.getContent()));
//                    default -> throw new IllegalArgumentException("Unsupported role: " + msg.getRole());
//                }
//            });
//        } catch (JsonProcessingException e) {
//            return Flux.error(e);
//        }
//
//        var requestId = UUID.randomUUID().toString();
//        var adviser = QuestionAnswerAdvisor.builder(vectorStore).searchRequest(SearchRequest.builder().topK(chatRequest.getOptions().getRagTopK()).build()).build();
//
//        var cancelDisposable = Schedulers.newSingle(requestId);
//        requestPool.put(requestId, cancelDisposable);
//        return chatClientBuilder.defaultOptions(ChatOptions.builder().model(options.getModel()).build()).build()
//                .prompt()
//                .advisors(adviser)
//                .messages(messages).stream().chatResponse()
//                .cancelOn(cancelDisposable)
//                .doFinally(signal -> clearRequest(requestId))
//                .onErrorStop()
//                .mapNotNull(o -> {
//                    if (o == null) return null;
//                    if (requestPool.containsKey(requestId))
//                        return ServerSentEvent.builder(GenUtils.objectToJsonSnake(toOpenAiLlmStreamResult(o, requestId))).id(requestId).event("spring-ai-llm").build();
//                    else
//                        return null;
//                })
//                .concatWith(Flux.just(DONE));
//    }

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

    @PostMapping("/api/chat")
    public Flux<ServerSentEvent<String>> chat(@RequestBody String requestText) {
        try {
            var chatRequest = objectMapper.readValue(requestText, ChatRequest.class);
            var request = chatRequest.getRequest();

            var requestId = IntegrationUtils.uuidV7().toString();

            var cancelDisposable = Schedulers.newSingle(requestId);
            requestPool.put(requestId, cancelDisposable);

            var messages = new LinkedList<org.springframework.ai.chat.messages.Message>();
            request.messages().forEach(msg -> {
                switch (msg.role()) {
                    case SYSTEM -> messages.add(new SystemMessage(msg.content()));
                    case USER -> messages.add(new UserMessage(msg.content()));
                    case ASSISTANT -> messages.add(new AssistantMessage(msg.content()));
                    default -> throw new IllegalArgumentException("Unsupported role: " + msg.role());
                }
            });

            return chatClientBuilder.defaultOptions(ChatOptions.builder().model(request.model()).build()).build()
                    .prompt(Prompt.builder().messages(messages).build())
                    .stream()
                    .chatResponse()
                    .cancelOn(cancelDisposable)
                    .doFinally(signal -> clearRequest(requestId))
                    .onErrorStop()
                    .mapNotNull(sse -> {
                        var o = sse.getResult().getOutput();
                        if (requestPool.containsKey(requestId))
                            return ServerSentEvent.builder(GenUtils.objectToJsonSnake(o)).id(requestId).build();
                        else
                            return null;
                    })
                    .concatWith(Flux.just(DONE));
        } catch (JsonProcessingException e) {
            return Flux.error(e);
        }

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

    @GetMapping(value = "/api/models", produces = "application/json")
    public Mono<String> listModels() {
        var usingOllama = System.getenv("USING_OLLAMA");
        if (usingOllama != null && usingOllama.equals("true")) {
            var ollamaUrl = System.getenv("OLLAMA_URL");
            if (ollamaUrl == null)
                ollamaUrl = "http://localhost:11434";
            return WebClient.create(ollamaUrl).get().uri("/api/tags").retrieve().bodyToMono(String.class);
        }
        return Mono.just("Hey");
//        return WebClient.create(ollamaUrl).get().uri("/api/tags").retrieve().bodyToMono(String.class);
    }
}
