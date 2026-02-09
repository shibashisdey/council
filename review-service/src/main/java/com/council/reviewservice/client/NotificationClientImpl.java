package com.council.reviewservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class NotificationClientImpl implements NotificationClient {

    private final WebClient webClient;

    public NotificationClientImpl(
            WebClient.Builder builder,
            @Value("${review.notification.base-url}") String notificationBaseUrl
    ) {
        this.webClient = builder.baseUrl(notificationBaseUrl).build();
    }

    @Override
    public void notifySessionNoteShared(Long sessionNoteId) {
        webClient.post()
                .uri("/notifications/session-note/{id}", sessionNoteId)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }
}
