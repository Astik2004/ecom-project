package com.astik.api_gateway.filter;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class JwtAuthenticationFilter extends
        AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    private static final List<String> PUBLIC_PATHS = List.of(
            // Auth paths
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh-token",
            "/api/v1/auth/verify-email",
            "/api/v1/auth/resend-verification",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/actuator/health",
            "/actuator/info",

            // Gateway Swagger
            "/swagger-ui",
            "/swagger-ui.html",
            "/webjars",
            "/v3/api-docs",

            // Services Swagger
            "/user-service/v1/api-docs",
            "/notification-service/v1/api-docs",
            "/product-service/v1/api-docs"
    );

    private static final Set<String> PUBLIC_GET_PREFIXES = Set.of(
            "/api/v1/products",
            "/api/v1/categories"
    );

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path   = request.getPath().value();
            String method = request.getMethod() != null
                    ? request.getMethod().name() : "GET";

            if (isPublicPath(path, method)) {
                return chain.filter(exchange);
            }

            String token = extractToken(request);
            if (!StringUtils.hasText(token)) {
                log.warn("Missing token | path={}", path);
                return errorResponse(exchange,
                        HttpStatus.UNAUTHORIZED, "Missing authorization token");
            }

            try {
                Claims claims = Jwts.parser()
                        .verifyWith(Keys.hmacShaKeyFor(
                                secretKey.getBytes(StandardCharsets.UTF_8)))
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String tokenType = claims.get("tokenType", String.class);
                if (!"ACCESS".equals(tokenType)) {
                    return errorResponse(exchange,
                            HttpStatus.UNAUTHORIZED, "Invalid token type");
                }

                String userId = claims.getSubject();
                String email  = claims.get("email", String.class);
                String role   = claims.get("role",  String.class);

                log.debug("Authenticated | userId={} role={} path={}",
                        userId, role, path);

                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-User-Id",      userId)
                        .header("X-User-Email",   email != null ? email : "")
                        .header("X-User-Role",    role  != null ? role  : "")
                        .header("X-Forwarded-By", "api-gateway")
                        .build();

                return chain.filter(exchange.mutate()
                        .request(mutatedRequest).build());

            } catch (ExpiredJwtException ex) {
                log.warn("Expired token | path={}", path);
                return errorResponse(exchange,
                        HttpStatus.UNAUTHORIZED, "Token has expired");
            } catch (JwtException ex) {
                log.warn("Invalid token | path={} error={}",
                        path, ex.getMessage());
                return errorResponse(exchange,
                        HttpStatus.UNAUTHORIZED, "Invalid token");
            } catch (Exception ex) {
                log.error("Token validation error | path={}", path, ex);
                return errorResponse(exchange,
                        HttpStatus.INTERNAL_SERVER_ERROR, "Authentication error");
            }
        };
    }

    private boolean isPublicPath(String path, String method) {
        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            return true;
        }
        if (HttpMethod.GET.name().equals(method)) {
            return PUBLIC_GET_PREFIXES.stream().anyMatch(path::startsWith);
        }
        return false;
    }

    private String extractToken(ServerHttpRequest request) {
        String header = request.getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }

    private Mono<Void> errorResponse(ServerWebExchange exchange,
                                     HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
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
        var buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    public static class Config {}
}