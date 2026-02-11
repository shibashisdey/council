package com.council.notificationservice.client;

import com.council.notificationservice.dto.AppointmentInternalResponse;
import com.council.notificationservice.security.InternalJwtService;
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
            @Value("${notification.appointment.base-url}") String appointmentBaseUrl
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
