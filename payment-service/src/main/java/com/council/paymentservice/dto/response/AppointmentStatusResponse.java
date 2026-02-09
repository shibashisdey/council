package com.council.paymentservice.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppointmentStatusResponse {
    private Long appointmentId;
    private String status;
}
