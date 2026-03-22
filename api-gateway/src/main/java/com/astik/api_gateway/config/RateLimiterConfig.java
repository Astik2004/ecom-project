package com.astik.api_gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String xff = exchange.getRequest()
                    .getHeaders().getFirst("X-Forwarded-For");
            String ip;
            if (xff != null && !xff.isEmpty()) {
                ip = xff.split(",")[0].trim();
            } else if (exchange.getRequest().getRemoteAddress() != null) {
                ip = exchange.getRequest().getRemoteAddress()
                        .getAddress().getHostAddress();
            } else {
                ip = "unknown";
            }
            return Mono.just("ip:" + ip);
        };
    }

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest()
                    .getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isEmpty()) {
                return Mono.just("user:" + userId);
            }
            if (exchange.getRequest().getRemoteAddress() != null) {
                return Mono.just("ip:" + exchange.getRequest()
                        .getRemoteAddress().getAddress().getHostAddress());
            }
            return Mono.just("ip:unknown");
        };
    }

    // ── YE 3 CHANGES HAIN ────────────────────────────────────────

    @Bean
    @Primary
    public RedisRateLimiter authRateLimiter() {
        return new RedisRateLimiter(10, 20, 1);
    }

    @Bean("publicRateLimiter")
    public RedisRateLimiter publicRateLimiter() {
        return new RedisRateLimiter(200, 400, 1);
    }

    @Bean("apiRateLimiter")
    public RedisRateLimiter apiRateLimiter() {
        return new RedisRateLimiter(100, 200, 1);
    }
}