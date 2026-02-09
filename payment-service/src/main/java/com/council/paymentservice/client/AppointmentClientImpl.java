package com.council.paymentservice.client;

import com.council.paymentservice.dto.response.AppointmentStatusResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class AppointmentClientImpl implements AppointmentClient {

    private final WebClient webClient;

    public AppointmentClientImpl(
            WebClient.Builder builder,
            @Value("${payment.appointment.base-url}") String appointmentBaseUrl
    ) {
        this.webClient = builder
                .baseUrl(appointmentBaseUrl)
                .build();
    }

    @Override
    public void confirmAppointment(Long appointmentId) {
        webClient.put()
                .uri("/appointments/{id}/confirm", appointmentId)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    @Override
    public AppointmentStatusResponse getAppointmentStatus(Long appointmentId) {
        return webClient.get()
                .uri("/appointments/{id}/status", appointmentId)
                .retrieve()
                .bodyToMono(AppointmentStatusResponse.class)
                .block();
    }
}
