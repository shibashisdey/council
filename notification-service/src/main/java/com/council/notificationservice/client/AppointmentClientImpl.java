package com.council.notificationservice.client;

import com.council.notificationservice.dto.AppointmentInternalResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class AppointmentClientImpl implements AppointmentClient {

    private final WebClient webClient;

    public AppointmentClientImpl(
            WebClient.Builder builder,
            @Value("${notification.appointment.base-url}") String appointmentBaseUrl
    ) {
        this.webClient = builder.baseUrl(appointmentBaseUrl).build();
    }

    @Override
    public AppointmentInternalResponse getAppointment(Long appointmentId) {
        return webClient.get()
                .uri("/appointments/{id}/internal", appointmentId)
                .retrieve()
                .onStatus(status -> status.value() == 404,
                        response -> reactor.core.publisher.Mono.error(
                                new IllegalArgumentException("Appointment not found")))
                .bodyToMono(AppointmentInternalResponse.class)
                .timeout(java.time.Duration.ofSeconds(5))
                .onErrorResume(IllegalArgumentException.class, ex -> reactor.core.publisher.Mono.empty())
                .block();
    }
}
