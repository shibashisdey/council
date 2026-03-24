package com.council.emailservice.client;

import com.council.emailservice.dto.AuthUserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class AuthClientImpl implements AuthClient {

    private final WebClient webClient;

    public AuthClientImpl(WebClient.Builder builder, @Value("${email.auth.base-url}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public AuthUserResponse getUserInternal(Long userId) {
        return webClient.get()
                .uri("/auth/users/{userId}/internal", userId)
                .retrieve()
                .bodyToMono(AuthUserResponse.class)
                .block();
    }
}
