package io.github.shangor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "custom")
@Data
public class CustomAiMcp {
    private Ai ai;
    private CustomMcp mcp;

    private String uploadDir;

    @Data
    public static class CustomMcp {
        private List<Server> remoteServers;

        @Data
        public static class Server {
            private String name;
            private String url;
        }
    }
    @Data
    public static class Ai {
        private Duration responseTimeout;
    }
}
