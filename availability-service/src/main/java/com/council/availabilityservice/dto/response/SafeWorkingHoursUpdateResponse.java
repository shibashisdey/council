package com.council.availabilityservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class SafeWorkingHoursUpdateResponse {

    private String status;

    private String message;

    private LocalDate effectiveFromDate;

    private int conflictCount;
}

