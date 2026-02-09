package com.council.appointmentservice.dto.response;

import com.council.appointmentservice.model.AppointmentStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AppointmentStatusResponse {
    private Long appointmentId;
    private AppointmentStatus status;
}
