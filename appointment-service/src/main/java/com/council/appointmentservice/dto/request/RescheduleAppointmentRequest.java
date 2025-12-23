package com.council.appointmentservice.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class RescheduleAppointmentRequest {

    private LocalDate newDate;

    private LocalTime newStartTime;
}
