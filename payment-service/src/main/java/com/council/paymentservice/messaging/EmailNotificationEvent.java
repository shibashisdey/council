package com.council.paymentservice.messaging;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
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
    private BigDecimal amount;
}
