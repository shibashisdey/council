package com.council.reviewservice.client;

import com.council.reviewservice.dto.response.AppointmentInternalResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class AppointmentClientImpl implements AppointmentClient {

    private final WebClient webClient;

    public AppointmentClientImpl(
            WebClient.Builder builder,
            @Value("${review.appointment.base-url}") String appointmentBaseUrl
    ) {
        this.webClient = builder.baseUrl(appointmentBaseUrl).build();
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
