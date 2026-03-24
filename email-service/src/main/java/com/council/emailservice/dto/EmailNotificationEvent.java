package com.council.emailservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class EmailNotificationEvent {
    private String eventType;
    private Instant occurredAt;
    private Long userId;
    private String userEmail;
    private String role;
    private Long appointmentId;
    private Long clientUserId;
    private Long counselorId;
    private String appointmentDate;
    private String startTime;
    private String endTime;
    private Long actorUserId;
    private String actorRole;
    private String pdfUrl;
    private BigDecimal amount;
}
