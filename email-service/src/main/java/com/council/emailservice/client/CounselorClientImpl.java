package com.council.emailservice.client;

import com.council.emailservice.dto.CounselorResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class CounselorClientImpl implements CounselorClient {

    private final WebClient webClient;

    public CounselorClientImpl(WebClient.Builder builder, @Value("${email.counselor.base-url}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public CounselorResponse getCounselor(Long counselorId) {
        return webClient.get()
                .uri("/counselors/{id}", counselorId)
                .retrieve()
                .bodyToMono(CounselorResponse.class)
                .block();
    }
}
