package io.github.shangor.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.autoconfigure.ollama.OllamaChatProperties;
import org.springframework.ai.autoconfigure.ollama.OllamaConnectionDetails;
import org.springframework.ai.autoconfigure.ollama.OllamaInitializationProperties;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.ollama.CustomOllamaChatModel;
import org.springframework.ai.ollama.api.CustomOllamaApi;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.ai.ollama.management.PullModelStrategy;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class CustomModelConfig {

    /**
     * Copy the logic from org.springframework.ai.autoconfigure.ollama.OllamaAutoConfiguration
     *
     */
    @Bean
    public CustomOllamaChatModel customOllamaChatModel(OllamaChatProperties properties,
                                                       OllamaInitializationProperties initProperties,
                                                       ToolCallingManager toolCallingManager,
                                                       ObjectProvider<ObservationRegistry> observationRegistry,
                                                       OllamaConnectionDetails connectionDetails,
                                                       ObjectProvider<ChatModelObservationConvention> observationConvention,
                                                       RestClient.Builder restClientBuilder,
                                                       WebClient.Builder webClientBuilder) {
        var chatModelPullStrategy = initProperties.getChat().isInclude() ? initProperties.getPullModelStrategy()
                : PullModelStrategy.NEVER;

        var modelManagementOptions = new ModelManagementOptions(chatModelPullStrategy, initProperties.getChat().getAdditionalModels(),
                initProperties.getTimeout(), initProperties.getMaxRetries());

        var ollamaApi = new CustomOllamaApi(connectionDetails.getBaseUrl(), restClientBuilder, webClientBuilder);
        var chatModel = new CustomOllamaChatModel(ollamaApi,
                properties.getOptions(),
                toolCallingManager,
                observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP),
                modelManagementOptions);
        observationConvention.ifAvailable(chatModel::setObservationConvention);
        return chatModel;
    }
}
