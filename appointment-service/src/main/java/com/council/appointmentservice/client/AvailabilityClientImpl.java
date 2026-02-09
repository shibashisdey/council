package com.council.appointmentservice.client;

import com.council.appointmentservice.dto.BlockSlotRequest;
import com.council.appointmentservice.dto.UpdateBlockReasonRequest;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
public class AvailabilityClientImpl implements AvailabilityClient {

    private final WebClient webClient;

    public AvailabilityClientImpl(WebClient.Builder webClientBuilder) {
        // In a real app, this URL would come from config and use service discovery
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(5))
                .followRedirect(true);

        this.webClient = webClientBuilder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl("http://localhost:8085/internal/availability").build();
    }

    @Override
    public boolean isSlotAvailable(Long counselorId, LocalDate date, LocalTime startTime) {
        Boolean result = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/check")
                        .queryParam("counselorId", counselorId)
                        .queryParam("date", date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                        .queryParam("startTime", startTime.format(DateTimeFormatter.ISO_LOCAL_TIME))
                        .build())
                .retrieve()
                .bodyToMono(Boolean.class)
                .block(); // Blocking for simplicity, can be made reactive

        return result != null && result;
    }

    @Override
    public void blockSlot(BlockSlotRequest request) {
        webClient.post()
                .uri("/block")
                .body(Mono.just(request), BlockSlotRequest.class)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    @Override
    public void freeSlot(Long referenceId) {
        webClient.post()
                .uri("/free/{referenceId}", referenceId)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    @Override
    public void updateBlockReason(Long referenceId, BlockSlotRequest.UnavailabilityReason newReason) {
        UpdateBlockReasonRequest request = new UpdateBlockReasonRequest();
        request.setNewReason(newReason);

        webClient.put()
                .uri("/block/{referenceId}/reason", referenceId)
                .body(Mono.just(request), UpdateBlockReasonRequest.class)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }
}
