package com.astik.api_gateway.config;

import com.astik.api_gateway.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.time.Duration;

@Configuration
public class GatewayConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final KeyResolver             ipKeyResolver;
    private final KeyResolver             userKeyResolver;
    private final RedisRateLimiter        authRateLimiter;
    private final RedisRateLimiter        publicRateLimiter;
    private final RedisRateLimiter        apiRateLimiter;

    public GatewayConfig(
            JwtAuthenticationFilter jwtFilter,
            @Qualifier("ipKeyResolver")     KeyResolver ipKeyResolver,
            @Qualifier("userKeyResolver")   KeyResolver userKeyResolver,
            @Qualifier("authRateLimiter")   RedisRateLimiter authRateLimiter,
            @Qualifier("publicRateLimiter") RedisRateLimiter publicRateLimiter,
            @Qualifier("apiRateLimiter")    RedisRateLimiter apiRateLimiter) {
        this.jwtFilter         = jwtFilter;
        this.ipKeyResolver     = ipKeyResolver;
        this.userKeyResolver   = userKeyResolver;
        this.authRateLimiter   = authRateLimiter;
        this.publicRateLimiter = publicRateLimiter;
        this.apiRateLimiter    = apiRateLimiter;
    }

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()

                // ==================== USER SERVICE ====================
                .route("user-service-auth", r -> r
                        .path("/api/v1/auth/**")
                        .filters(f -> f
                                .filter(jwtFilter.apply(
                                        new JwtAuthenticationFilter.Config()))
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(authRateLimiter)
                                        .setKeyResolver(ipKeyResolver)
                                        .setDenyEmptyKey(false)
                                        .setStatusCode(
                                                HttpStatus.TOO_MANY_REQUESTS))
                                .retry(rc -> rc
                                        .setRetries(2)
                                        .setStatuses(
                                                HttpStatus.BAD_GATEWAY,
                                                HttpStatus.SERVICE_UNAVAILABLE,
                                                HttpStatus.GATEWAY_TIMEOUT)
                                        .setBackoff(
                                                Duration.ofMillis(100),
                                                Duration.ofMillis(500),
                                                2, true))
                                .removeRequestHeader("Cookie")
                                .addResponseHeader(
                                        "X-Content-Type-Options", "nosniff")
                                .addResponseHeader(
                                        "X-Frame-Options", "DENY"))
                        .uri("lb://user-service"))

                .route("user-service-users", r -> r
                        .path("/api/v1/users/**")
                        .filters(f -> f
                                .filter(jwtFilter.apply(
                                        new JwtAuthenticationFilter.Config()))
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(apiRateLimiter)
                                        .setKeyResolver(userKeyResolver)
                                        .setDenyEmptyKey(false)
                                        .setStatusCode(
                                                HttpStatus.TOO_MANY_REQUESTS))
                                .retry(rc -> rc
                                        .setRetries(2)
                                        .setStatuses(
                                                HttpStatus.BAD_GATEWAY,
                                                HttpStatus.SERVICE_UNAVAILABLE)
                                        .setBackoff(
                                                Duration.ofMillis(100),
                                                Duration.ofMillis(500),
                                                2, true))
                                .removeRequestHeader("Cookie")
                                .addResponseHeader(
                                        "X-Content-Type-Options", "nosniff"))
                        .uri("lb://user-service"))

                // ==================== PRODUCT SERVICE ====================
                .route("product-service-public", r -> r
                        .path("/api/v1/products/**",
                                "/api/v1/categories/**")
                        .and().method("GET")
                        .filters(f -> f
                                .filter(jwtFilter.apply(
                                        new JwtAuthenticationFilter.Config()))
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(publicRateLimiter)
                                        .setKeyResolver(ipKeyResolver)
                                        .setDenyEmptyKey(false)
                                        .setStatusCode(
                                                HttpStatus.TOO_MANY_REQUESTS))
                                .retry(rc -> rc
                                        .setRetries(3)
                                        .setStatuses(
                                                HttpStatus.BAD_GATEWAY,
                                                HttpStatus.SERVICE_UNAVAILABLE)
                                        .setBackoff(
                                                Duration.ofMillis(50),
                                                Duration.ofMillis(300),
                                                2, true))
                                .removeRequestHeader("Cookie")
                                .addResponseHeader(
                                        "Cache-Control", "public, max-age=60"))
                        .uri("lb://product-service"))

                .route("product-service-write", r -> r
                        .path("/api/v1/products/**",
                                "/api/v1/categories/**")
                        .and().method("POST", "PUT", "DELETE", "PATCH")
                        .filters(f -> f
                                .filter(jwtFilter.apply(
                                        new JwtAuthenticationFilter.Config()))
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(apiRateLimiter)
                                        .setKeyResolver(userKeyResolver)
                                        .setDenyEmptyKey(false)
                                        .setStatusCode(
                                                HttpStatus.TOO_MANY_REQUESTS))
                                .removeRequestHeader("Cookie")
                                .addResponseHeader(
                                        "X-Content-Type-Options", "nosniff"))
                        .uri("lb://product-service"))

                // ==================== SWAGGER DOCS ROUTES ====================
                .route("user-service-docs", r -> r
                        .path("/user-service/v1/api-docs",
                                "/user-service/v1/api-docs/**")
                        .filters(f -> f
                                .rewritePath(
                                        "/user-service/(?<segment>.*)",
                                        "/${segment}"))
                        .uri("lb://user-service"))

                .route("notification-service-docs", r -> r
                        .path("/notification-service/v1/api-docs",
                                "/notification-service/v1/api-docs/**")
                        .filters(f -> f
                                .rewritePath(
                                        "/notification-service/(?<segment>.*)",
                                        "/${segment}"))
                        .uri("lb://notification-service"))

                .route("product-service-docs", r -> r
                        .path("/product-service/v1/api-docs",
                                "/product-service/v1/api-docs/**")
                        .filters(f -> f
                                .rewritePath(
                                        "/product-service/(?<segment>.*)",
                                        "/${segment}"))
                        .uri("lb://product-service"))

                .build();
    }
}