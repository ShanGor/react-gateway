package io.github.shangor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
@EnableWebFlux
public class WebConfig implements WebFluxConfigurer {
    private final String corsAllowedOrigin;

    public WebConfig(@Value("${cors.allowed-origin}") String corsAllowedOrigin) {
        this.corsAllowedOrigin = corsAllowedOrigin;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(corsAllowedOrigin)
                .allowedMethods("GET", "OPTIONS", "POST", "PUT", "DELETE")
                .allowCredentials(true);
    }
}