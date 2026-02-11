package com.council.notificationservice.client;

import com.council.notificationservice.dto.SessionNotePublicResponse;
import com.council.notificationservice.dto.UpdatePdfRequest;
import com.council.notificationservice.security.InternalJwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class ReviewClientImpl implements ReviewClient {

    private final WebClient webClient;

    public ReviewClientImpl(
            WebClient.Builder builder,
            InternalJwtService internalJwtService,
            @Value("${notification.review.base-url}") String reviewBaseUrl
    ) {
        ExchangeFilterFunction authFilter = (request, next) -> next.exchange(
                org.springframework.web.reactive.function.client.ClientRequest.from(request)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + internalJwtService.generateToken())
                        .build()
        );
        this.webClient = builder.baseUrl(reviewBaseUrl)
                .filter(authFilter)
                .build();
    }

    @Override
    public SessionNotePublicResponse getSessionNote(Long noteId) {
        return webClient.get()
                .uri("/session-notes/{id}/internal", noteId)
                .retrieve()
                .onStatus(status -> status.value() == 404,
                        response -> reactor.core.publisher.Mono.error(
                                new IllegalArgumentException("Session note not found")))
                .bodyToMono(SessionNotePublicResponse.class)
                .timeout(java.time.Duration.ofSeconds(5))
                .block();
    }

    @Override
    public void updatePdf(Long noteId, UpdatePdfRequest request) {
        webClient.patch()
                .uri("/session-notes/{id}/pdf", noteId)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class)
                .timeout(java.time.Duration.ofSeconds(5))
                .block();
    }
}
