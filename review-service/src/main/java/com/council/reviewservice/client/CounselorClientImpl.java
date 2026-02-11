package com.council.reviewservice.client;

import com.council.reviewservice.dto.response.CounselorResponse;
import com.council.reviewservice.security.InternalJwtService;
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
            @Value("${review.counselor.base-url}") String counselorBaseUrl
    ) {
        ExchangeFilterFunction authFilter = (request, next) -> next.exchange(
                org.springframework.web.reactive.function.client.ClientRequest.from(request)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + internalJwtService.generateToken())
                        .build()
        );

        this.webClient = builder
                .baseUrl(counselorBaseUrl)
                .filter(authFilter)
                .build();
    }

    @Override
    public CounselorResponse getCounselorByUserId(Long userId) {
        return webClient.get()
                .uri("/counselors/user/{userId}", userId)
                .retrieve()
                .bodyToMono(CounselorResponse.class)
                .block();
    }
}
