package com.council.availabilityservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class CounselorAvailabilityResponse {

    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status; // AVAILABLE / UNAVAILABLE
    private String reason;
}
