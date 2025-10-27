package io.github.shangor.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.shangor.data.dto.ChatRequest;
import io.github.shangor.data.dto.LlmOptions;
import io.github.shangor.data.dto.OpenAiRequestMessage;

import io.github.shangor.service.LlmService;
import io.github.shangor.util.IntegrationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@Slf4j
public class ChatController {

    private final ObjectMapper objectMapper;

    private final Map<String, Boolean> requestPool = new ConcurrentHashMap<>();

    private final LlmService llmService;

    private static final ServerSentEvent<String> DONE = ServerSentEvent.builder("DONE").build();

    public ChatController(ObjectMapper objectMapper, LlmService llmService) {
        this.objectMapper = objectMapper;
        this.llmService = llmService;
    }


    @PostMapping("/api/chat")
    public Flux<ServerSentEvent<String>> chat(@RequestBody String requestText) {
        try {
            var req = objectMapper.readValue(requestText, ChatRequest.class);
            var requestId = IntegrationUtils.uuidV7().toString();
            log.info("Received request: {}", requestId);
            var messagesBuilder = OpenAiRequestMessage.builder()
                    .role("user");
            if (req.getMediaDataUrls() != null && !req.getMediaDataUrls().isEmpty()) {
                var list = new ArrayList<OpenAiRequestMessage.Content>();
                list.add(OpenAiRequestMessage.Content.builder()
                        .type("text")
                        .text(req.getPrompt())
                        .build());
                for (var url : req.getMediaDataUrls()) {
                    list.add(OpenAiRequestMessage.Content.builder()
                            .type("image_url")
                            .image_url(url)
                            .build());
                }
                messagesBuilder = messagesBuilder.content(list);
            } else {
                messagesBuilder = messagesBuilder.content(req.getPrompt());
            }

            var message = messagesBuilder.build();
            var options = LlmOptions.builder()
                    .model(req.getModel())
                    .temperature(req.getTemperature())
                    .maxCompletionTokens(req.getMaxCompletionTokens())
                    .build();
            return Flux.create(sink ->
                Thread.ofVirtual().name("llm-" + requestId).start(() -> {
                    requestPool.put(requestId, true);

                    var resp = llmService.chatCompletion(options, List.of(message), null)
                            .takeWhile(ignore -> requestPool.containsKey(requestId))
                            .map(chunk ->
                                    sink.next(ServerSentEvent.builder(chunk).id(requestId).event("llm").build())
                            );

                    resp.blockLast();
                    requestPool.remove(requestId);
                    sink.complete();
                    log.info("Request {} ended", requestId);
                })
            );
        } catch (JsonProcessingException e) {
            return Flux.error(e);
        }

    }

    private void clearRequest(String requestId) {
        requestPool.computeIfPresent(requestId, (k, v) -> {
            try {
                requestPool.remove(requestId);
                log.info("Successfully cancelled request {}", requestId);
            } catch (Exception e) {
                log.error("Failed to clear request {}: {}", requestId, e.getMessage());
            }
            return null;
        });

    }

    @GetMapping(value = "/api/cancel/{requestId}")
    public String cancel(@PathVariable String requestId) {
        clearRequest(requestId);
        return "ok";
    }


}
