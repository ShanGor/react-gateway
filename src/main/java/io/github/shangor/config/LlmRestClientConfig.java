package io.github.shangor.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * We found that the spring ai client cannot config timeout, so we use RestClient to config it.
 */
@Configuration
@Slf4j
public class LlmRestClientConfig {

    /**
     * in big companies, they might use a short-living API key to access LLM services, like changes every minute. We need to be able to refresh it.
     * This `authKey()` method is for you to implement your own logic to get the authKey.
     */
    public String authKey() {
        var dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        var key = "sk-" + dtf.format(LocalDateTime.now());
        log.info("Pretending to be using authKey: {}", key);
        return key;
    }

    @Bean
    RestClient.Builder llmRestClient(CustomAiMcp ai) {
        var responseTimeout = ai.getAi().getResponseTimeout();
        log.info("llmRestClient responseTimeout: {} seconds", responseTimeout.getSeconds());
        HttpClient client = HttpClient.newConnection() // no connection pool
                .responseTimeout(responseTimeout);
        ClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory(client);

        return RestClient.builder()
                .requestFactory(factory);
    }

    @Bean("llmWebClient")
    WebClient.Builder llmWebClient(CustomAiMcp ai) {
        var responseTimeout = ai.getAi().getResponseTimeout();
        log.info("llmWebClient responseTimeout: {} seconds", responseTimeout.getSeconds());
        HttpClient client = HttpClient.newConnection() // no connection pool
                .responseTimeout(responseTimeout);

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(client));
    }
}
