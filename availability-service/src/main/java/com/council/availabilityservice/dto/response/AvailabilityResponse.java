package com.council.availabilityservice.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AvailabilityResponse {
    private boolean available;
    private String reason;
}
