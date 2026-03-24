package com.council.userauthenticationservice.messaging;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class EmailNotificationEvent {
    private String eventType;
    private Instant occurredAt;
    private Long userId;
    private String userEmail;
    private String role;
}
