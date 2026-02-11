package com.council.notificationservice.client;

import com.council.notificationservice.dto.CounselorResponse;
import com.council.notificationservice.security.InternalJwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class CounselorClientImpl implements CounselorClient {

    private final WebClient webClient;

    public CounselorClientImpl(
            WebClient.Builder builder,
            InternalJwtService internalJwtService,
            @Value("${notification.counselor.base-url}") String counselorBaseUrl
    ) {
        ExchangeFilterFunction authFilter = (request, next) -> next.exchange(
                org.springframework.web.reactive.function.client.ClientRequest.from(request)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + internalJwtService.generateToken())
                        .build()
        );
        this.webClient = builder.baseUrl(counselorBaseUrl)
                .filter(authFilter)
                .build();
    }

    @Override
    public CounselorResponse getCounselorById(Long counselorId) {
        return webClient.get()
                .uri("/counselors/{counselorId}", counselorId)
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
