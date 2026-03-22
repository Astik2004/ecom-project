package com.astik.api_gateway.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Component
@Order(-1)
@Slf4j
public class GatewayExceptionHandler
        implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange,
                             Throwable ex) {
        var response = exchange.getResponse();
        String path  = exchange.getRequest().getPath().value();

        HttpStatus status;
        String message;

        if (ex instanceof NotFoundException) {
            status  = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Service is temporarily unavailable";
            log.error("Service not found | path={}", path);
        } else if (ex instanceof ResponseStatusException rse) {
            status  = HttpStatus.valueOf(rse.getStatusCode().value());
            message = rse.getReason() != null
                    ? rse.getReason() : "Request failed";
            log.warn("Status exception | path={} status={}", path, status);
        } else {
            status  = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "An unexpected error occurred";
            log.error("Unhandled error | path={}", path, ex);
        }

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {
                  "success": false,
                  "message": "%s",
                  "data": null,
                  "timestamp": "%s"
                }
                """.formatted(message, LocalDateTime.now());

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        var buffer   = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}