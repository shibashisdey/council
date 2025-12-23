package com.council.appointmentservice.dto.response;

import com.council.appointmentservice.model.AppointmentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class CounselorAppointmentResponse {

    private Long appointmentId;

    private Long clientId;

    private LocalDate appointmentDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private AppointmentStatus status;
}