package com.astik.api_gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Component
@Slf4j
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        long startTime   = Instant.now().toEpochMilli();

        exchange.getAttributes().put("requestId", requestId);
        exchange.getAttributes().put("startTime", startTime);

        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-Request-Id", requestId)
                .build();

        log.info("→ IN  | id={} method={} path={} ip={}",
                requestId,
                request.getMethod(),
                request.getPath().value(),
                getClientIp(request));

        return chain.filter(exchange.mutate()
                        .request(mutatedRequest).build())
                .then(Mono.fromRunnable(() -> {
                    long duration = Instant.now().toEpochMilli() - startTime;
                    int status = exchange.getResponse()
                            .getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value()
                            : 0;

                    log.info("← OUT | id={} status={} duration={}ms",
                            requestId, status, duration);
                }));
    }

    private String getClientIp(ServerHttpRequest request) {
        String xff = request.getHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress()
                    .getAddress().getHostAddress();
        }
        return "unknown";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}