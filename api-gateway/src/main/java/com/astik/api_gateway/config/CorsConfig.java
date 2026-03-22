package com.astik.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(List.of("*")); // sabhi origins allow karo

        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT",
                "DELETE", "PATCH", "OPTIONS"));

        config.setAllowedHeaders(List.of("*")); // sabhi headers allow karo

        config.setExposedHeaders(List.of(
                "X-Request-Id",
                "X-RateLimit-Remaining",
                "X-RateLimit-Replenish-Rate",
                "X-RateLimit-Burst-Capacity",
                "Authorization"));

        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}