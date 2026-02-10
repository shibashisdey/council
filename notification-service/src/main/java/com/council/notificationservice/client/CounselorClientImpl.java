package com.council.notificationservice.client;

import com.council.notificationservice.dto.CounselorResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class CounselorClientImpl implements CounselorClient {

    private final WebClient webClient;

    public CounselorClientImpl(
            WebClient.Builder builder,
            @Value("${notification.counselor.base-url}") String counselorBaseUrl
    ) {
        this.webClient = builder.baseUrl(counselorBaseUrl).build();
    }

    @Override
    public CounselorResponse getCounselorByUserId(Long userId) {
        return webClient.get()
                .uri("/counselors/user/{userId}", userId)
                .retrieve()
                .onStatus(status -> status.value() == 404,
                        response -> reactor.core.publisher.Mono.error(
                                new IllegalArgumentException("Counselor not found")))
                .bodyToMono(CounselorResponse.class)
                .timeout(java.time.Duration.ofSeconds(5))
                .onErrorResume(IllegalArgumentException.class, ex -> reactor.core.publisher.Mono.empty())
                .block();
    }
}
