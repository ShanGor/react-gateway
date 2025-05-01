package io.github.shangor.config;

import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
@EnableWebFlux
@Slf4j
public class WebConfig implements WebFluxConfigurer {
    private final String corsAllowedOrigin;

    public WebConfig(@Value("${cors.allowed-origin:}") String corsAllowedOrigin) {
        this.corsAllowedOrigin = corsAllowedOrigin;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (StringUtils.isNotBlank(corsAllowedOrigin)) {
            log.info("CORS enabling for origin: {}", corsAllowedOrigin);

            registry.addMapping("/api/**")
                    .allowedOrigins(corsAllowedOrigin)
                    .allowedMethods("GET", "OPTIONS", "POST", "PUT", "DELETE", "PATCH")
                    .allowCredentials(true);
        }
    }
}
