package com.council.notificationservice.client;

import com.council.notificationservice.dto.SessionNotePublicResponse;
import com.council.notificationservice.dto.UpdatePdfRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class ReviewClientImpl implements ReviewClient {

    private final WebClient webClient;

    public ReviewClientImpl(
            WebClient.Builder builder,
            @Value("${notification.review.base-url}") String reviewBaseUrl
    ) {
        this.webClient = builder.baseUrl(reviewBaseUrl).build();
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
