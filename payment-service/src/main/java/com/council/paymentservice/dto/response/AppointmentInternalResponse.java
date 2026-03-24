package com.council.paymentservice.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppointmentInternalResponse {
    private Long appointmentId;
    private Long clientId;
    private Long counselorId;
    private String appointmentDate;
    private String startTime;
    private String endTime;
    private String status;
}
