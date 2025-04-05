package org.springframework.ai.ollama.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;


public class CustomOllamaApi extends OllamaApi {

    private static final Log logger = LogFactory.getLog(CustomOllamaApi.class);

    private final WebClient webClient;

    public CustomOllamaApi(String baseUrl, RestClient.Builder restClientBuilder, WebClient.Builder webClientBuilder) {
        super(baseUrl, restClientBuilder, webClientBuilder);
        Consumer<HttpHeaders> defaultHeaders = headers -> {
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        };

        this.webClient = webClientBuilder.baseUrl(baseUrl).defaultHeaders(defaultHeaders).build();
    }
    public Flux<ChatResponse> streamingChat(ChatRequest chatRequest, Consumer<ChatResponse> streamObserver) {
        Assert.notNull(chatRequest, REQUEST_BODY_NULL_ERROR);
        Assert.isTrue(chatRequest.stream(), "Request must set the stream property to true.");

        AtomicBoolean isInsideTool = new AtomicBoolean(false);

        return this.webClient.post()
                .uri("/api/chat")
                .body(Mono.just(chatRequest), ChatRequest.class)
                .retrieve()
                .bodyToFlux(ChatResponse.class)
                .map(chunk -> {
                    if (OllamaApiHelper.isStreamingToolCall(chunk)) {
                        isInsideTool.set(true);
                    }
                    if (streamObserver != null) {
                        streamObserver.accept(chunk);
                    }
                    return chunk;
                })
                // Group all chunks belonging to the same function call.
                // Flux<ChatChatResponse> -> Flux<Flux<ChatChatResponse>>
                .windowUntil(chunk -> {
                    if (isInsideTool.get() && OllamaApiHelper.isStreamingDone(chunk)) {
                        isInsideTool.set(false);
                        return true;
                    }
                    return !isInsideTool.get();
                })
                // Merging the window chunks into a single chunk.
                // Reduce the inner Flux<ChatChatResponse> window into a single
                // Mono<ChatChatResponse>,
                // Flux<Flux<ChatChatResponse>> -> Flux<Mono<ChatChatResponse>>
                .concatMapIterable(window -> {
                    Mono<ChatResponse> monoChunk = window.reduce(
                            new ChatResponse(),
                            (previous, current) -> OllamaApiHelper.merge(previous, current));
                    return List.of(monoChunk);
                })
                // Flux<Mono<ChatChatResponse>> -> Flux<ChatChatResponse>
                .flatMap(mono -> mono)
                .handle((data, sink) -> {
                    if (logger.isTraceEnabled()) {
                        logger.trace(data);
                    }
                    sink.next(data);
                });
    }

}
