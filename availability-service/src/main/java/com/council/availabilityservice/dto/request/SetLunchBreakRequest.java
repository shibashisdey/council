package com.council.availabilityservice.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class SetLunchBreakRequest {
    private LocalTime startTime;
    private LocalTime endTime;
}
