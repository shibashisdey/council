package com.council.notificationservice.messaging;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class EmailNotificationEvent {
    private String eventType;
    private Instant occurredAt;
    private Long appointmentId;
    private Long clientUserId;
    private Long counselorId;
    private String appointmentDate;
    private String startTime;
    private String endTime;
    private String pdfUrl;
}
