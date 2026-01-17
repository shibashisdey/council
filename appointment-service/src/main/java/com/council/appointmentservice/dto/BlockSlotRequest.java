package com.council.appointmentservice.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
public class BlockSlotRequest {

    private Long counselorId;
    private LocalDate date;
    private LocalTime startTime;
    private UnavailabilityReason reason;
    private Long referenceId;

    // A simple enum mirror of the one in availability-service
    public enum UnavailabilityReason {
        APPOINTMENT_HOLD,
        APPOINTMENT_CONFIRMED
    }
}
