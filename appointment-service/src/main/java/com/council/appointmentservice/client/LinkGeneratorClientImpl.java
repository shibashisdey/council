package com.council.appointmentservice.client;

import com.council.appointmentservice.dto.MeetingLinkRequest;
import com.council.appointmentservice.dto.MeetingLinkResponse;
import com.council.appointmentservice.dto.MeetingLinkUpdateRequest;
import com.council.appointmentservice.security.InternalJwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Service
public class LinkGeneratorClientImpl implements LinkGeneratorClient {

    private final WebClient webClient;

    public LinkGeneratorClientImpl(
            WebClient.Builder webClientBuilder,
            InternalJwtService internalJwtService,
            @Value("${appointment.link-generator.base-url}") String baseUrl
    ) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(5))
                .followRedirect(true);

        ExchangeFilterFunction authFilter = (request, next) -> next.exchange(
                org.springframework.web.reactive.function.client.ClientRequest.from(request)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + internalJwtService.generateToken())
                        .build()
        );

        this.webClient = webClientBuilder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(baseUrl + "/internal/meeting-links")
                .filter(authFilter)
                .build();
    }

    @Override
    public MeetingLinkResponse createOrGet(MeetingLinkRequest request) {
        return webClient.post()
                .uri("")
                .body(Mono.just(request), MeetingLinkRequest.class)
                .retrieve()
                .bodyToMono(MeetingLinkResponse.class)
                .block();
    }

    @Override
    public MeetingLinkResponse getByAppointmentId(Long appointmentId) {
        return webClient.get()
                .uri("/{appointmentId}", appointmentId)
                .retrieve()
                .bodyToMono(MeetingLinkResponse.class)
                .block();
    }

    @Override
    public MeetingLinkResponse updateMeetingLink(Long appointmentId, MeetingLinkUpdateRequest request) {
        return webClient.put()
                .uri("/{appointmentId}", appointmentId)
                .body(Mono.just(request), MeetingLinkUpdateRequest.class)
                .retrieve()
                .bodyToMono(MeetingLinkResponse.class)
                .block();
    }

    @Override
    public void deleteMeetingLink(Long appointmentId) {
        webClient.delete()
                .uri("/{appointmentId}", appointmentId)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }
}
