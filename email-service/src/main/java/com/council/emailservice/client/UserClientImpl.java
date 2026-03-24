package com.council.emailservice.client;

import com.council.emailservice.dto.UserProfileResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class UserClientImpl implements UserClient {

    private final WebClient webClient;

    public UserClientImpl(WebClient.Builder builder, @Value("${email.user.base-url}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public UserProfileResponse getUserPublic(Long userId) {
        return webClient.get()
                .uri("/users/{userId}/public", userId)
                .retrieve()
                .bodyToMono(UserProfileResponse.class)
                .block();
    }
}
