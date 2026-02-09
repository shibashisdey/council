package com.council.apigateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final SecretKey secretKey;

    public JwtAuthenticationFilter(
            @Value("${jwt.secret.key}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        // Allow auth endpoints
        if (path.startsWith("/auth")) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        try {
            var claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String email = claims.getSubject();
            String role = claims.get("role", String.class);

            Long userId = claims.get("userId", Integer.class).longValue();

            // 🔐 ROLE-BASED AUTHORIZATION (ADD HERE)
            if (path.startsWith("/counselors")) {

                String method = exchange.getRequest().getMethod().name();


                // Only THERAPIST can create or update counselor profile
                if ((method.equals("POST") || method.equals("PUT")|| method.equals("PATCH"))
                        && !"THERAPIST".equals(role)) {

                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().setComplete();
                }
                // Read operations → CLIENT or THERAPIST
                if (method.equals("GET")
                        && !("CLIENT".equals(role) || "THERAPIST".equals(role))) {
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().setComplete();
                }

            }

            if (path.startsWith("/users")) {
                if (!"CLIENT".equals(role)) {
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().setComplete();
                }
            }


            // Attach trusted headers for downstream services
            exchange = exchange.mutate()
                    .request(r -> r
                            .header("X-USER-ID", String.valueOf(userId))
                            .header("X-USER-EMAIL", email)
                            .header("X-USER-ROLE", role)
                    )
                    .build();

        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }


        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
