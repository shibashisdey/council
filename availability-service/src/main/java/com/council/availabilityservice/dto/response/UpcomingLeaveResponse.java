package com.council.availabilityservice.dto.response;

import com.council.availabilityservice.model.UnavailabilityReason;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class UpcomingLeaveResponse {
    private Long id;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private UnavailabilityReason reason;
}
