package com.council.appointmentservice.dto.response;

import com.council.appointmentservice.model.AppointmentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Builder
public class AppointmentResponse {

    private Long appointmentId;

    private Long clientId;

    private String clientName;       // 👈 from user-auth service

    private Long counselorId;

    private String counselorName;     // 👈 from counselor-service

    private LocalDate appointmentDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private AppointmentStatus status;

    private String paymentId;

    private LocalDateTime createdAt;

    private String meetingLink;
}
