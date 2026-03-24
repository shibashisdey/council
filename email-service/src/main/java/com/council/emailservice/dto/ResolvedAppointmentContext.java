package com.council.emailservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResolvedAppointmentContext {
    private Long appointmentId;
    private String appointmentDate;
    private String startTime;
    private String endTime;
    private Long clientUserId;
    private String clientEmail;
    private String clientName;
    private Long counselorId;
    private Long counselorUserId;
    private String counselorEmail;
    private String counselorName;
}
