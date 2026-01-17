package com.council.availabilityservice.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class CheckAvailabilityRequest {

    private Long counselorId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
}
