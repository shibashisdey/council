package com.council.appointmentservice.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class CreateAppointmentRequest {

    /**
     * Counselor ID (from counselor-service)
     */
    private Long counselorId;

    /**
     * Appointment date
     */
    private LocalDate appointmentDate;

    /**
     * Slot start time (1-hour slot)
     */
    private LocalTime startTime;
}

