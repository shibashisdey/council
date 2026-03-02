package com.council.availabilityservice.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Setter
public class SafeWorkingHoursUpdateRequest {

    private DayOfWeek dayOfWeek;

    private LocalTime startTime;

    private LocalTime endTime;

    private LocalTime lunchStartTime;

    private LocalTime lunchEndTime;
}

