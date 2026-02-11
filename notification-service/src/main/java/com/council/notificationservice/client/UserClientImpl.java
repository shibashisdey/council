package com.council.notificationservice.client;

import com.council.notificationservice.dto.UserPublicResponse;
import com.council.notificationservice.security.InternalJwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class UserClientImpl implements UserClient {

    private final WebClient webClient;

    public UserClientImpl(
            WebClient.Builder builder,
            InternalJwtService internalJwtService,
            @Value("${notification.user.base-url}") String userBaseUrl
    ) {
        ExchangeFilterFunction authFilter = (request, next) -> next.exchange(
                org.springframework.web.reactive.function.client.ClientRequest.from(request)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + internalJwtService.generateToken())
                        .build()
        );
        this.webClient = builder.baseUrl(userBaseUrl)
                .filter(authFilter)
                .build();
    }

    @Override
    public UserPublicResponse getUserPublic(Long userId) {
        return webClient.get()
                .uri("/users/{id}/public", userId)
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
