package com.council.availabilityservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalTime;

@Getter
@Builder
public class LunchBreakResponse {

    private LocalTime startTime;

    private LocalTime endTime;
}
