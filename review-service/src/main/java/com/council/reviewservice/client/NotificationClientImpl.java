package com.council.reviewservice.client;

import com.council.reviewservice.dto.request.NotifySessionNoteRequest;
import com.council.reviewservice.security.InternalJwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class NotificationClientImpl implements NotificationClient {

    private final WebClient webClient;

    public NotificationClientImpl(
            WebClient.Builder builder,
            InternalJwtService internalJwtService,
            @Value("${review.notification.base-url}") String notificationBaseUrl
    ) {
        ExchangeFilterFunction authFilter = (request, next) -> next.exchange(
                org.springframework.web.reactive.function.client.ClientRequest.from(request)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + internalJwtService.generateToken())
                        .build()
        );
        this.webClient = builder.baseUrl(notificationBaseUrl)
                .filter(authFilter)
                .build();
    }

    @Override
    public void notifySessionNoteShared(Long sessionNoteId) {
        webClient.post()
                .uri("/notifications/session-note/{id}", sessionNoteId)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    @Override
    public void notifySessionNoteSharedWithContent(NotifySessionNoteRequest request) {
        webClient.post()
                .uri("/notifications/session-note")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }
}
