package com.council.notificationservice.client;

import com.council.notificationservice.dto.UserPublicResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class UserClientImpl implements UserClient {

    private final WebClient webClient;

    public UserClientImpl(
            WebClient.Builder builder,
            @Value("${notification.user.base-url}") String userBaseUrl
    ) {
        this.webClient = builder.baseUrl(userBaseUrl).build();
    }

    @Override
    public UserPublicResponse getUserPublic(Long userId) {
        return webClient.get()
                .uri("/users/{id}/public", userId)
                .header("X-INTERNAL-CALL", "true")
                .retrieve()
                .onStatus(status -> status.value() == 404,
                        response -> reactor.core.publisher.Mono.error(
                                new IllegalArgumentException("User not found")))
                .bodyToMono(UserPublicResponse.class)
                .timeout(java.time.Duration.ofSeconds(5))
                .onErrorResume(IllegalArgumentException.class, ex -> reactor.core.publisher.Mono.empty())
                .block();
    }
}
