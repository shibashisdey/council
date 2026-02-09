package com.council.reviewservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReviewResponse {
    private Long id;
    private Long appointmentId;
    private Long userId;
    private Long counselorId;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;
}
