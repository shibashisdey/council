package com.council.availabilityservice.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class AddUnavailabilityRequest {

    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String reason; // enum name
}
