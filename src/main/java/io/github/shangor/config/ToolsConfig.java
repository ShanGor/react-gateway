package io.github.shangor.config;

import io.github.shangor.service.FairyTaleService;
import io.github.shangor.service.FilingService;
import io.modelcontextprotocol.client.McpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

@Configuration
//@RequiredArgsConstructor
public class ToolsConfig {
//    private final FairyTaleService fairyTaleService;
//    private final FilingService filingService;
//
//    @Bean
//    public ToolCallbackProvider tools() {
//        return MethodToolCallbackProvider.builder()
//                .toolObjects(fairyTaleService, filingService)
//                .build();
//    }

//    private final ConfigurableEnvironment environment;
//
//    private final ConfigurableApplicationContext context;
//
//    public ToolsConfig(ConfigurableEnvironment environment, ConfigurableApplicationContext context) {
//        this.environment = environment;
//        this.context = context;
//    }
//    @Bean
//    @DependsOn("mcpServer")
//    public McpClient mcpClient(McpClient existingClient) {
//        return existingClient;
//    }
}
