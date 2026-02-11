package com.council.reviewservice.client;

import com.council.reviewservice.dto.response.AppointmentInternalResponse;
import com.council.reviewservice.security.InternalJwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class AppointmentClientImpl implements AppointmentClient {

    private final WebClient webClient;

    public AppointmentClientImpl(
            WebClient.Builder builder,
            InternalJwtService internalJwtService,
            @Value("${review.appointment.base-url}") String appointmentBaseUrl
    ) {
        ExchangeFilterFunction authFilter = (request, next) -> next.exchange(
                org.springframework.web.reactive.function.client.ClientRequest.from(request)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + internalJwtService.generateToken())
                        .build()
        );
        this.webClient = builder.baseUrl(appointmentBaseUrl)
                .filter(authFilter)
                .build();
    }

    @Override
    public AppointmentInternalResponse getAppointmentInternal(Long appointmentId) {
        return webClient.get()
                .uri("/appointments/{id}/internal", appointmentId)
                .retrieve()
                .bodyToMono(AppointmentInternalResponse.class)
                .block();
    }

    @Override
    public void completeAppointment(Long appointmentId) {
        webClient.put()
                .uri("/appointments/{id}/complete", appointmentId)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }
}
