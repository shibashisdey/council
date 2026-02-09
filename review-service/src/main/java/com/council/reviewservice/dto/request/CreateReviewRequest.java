package com.council.reviewservice.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReviewRequest {
    private Long appointmentId;
    private int rating;
    private String comment;
}
