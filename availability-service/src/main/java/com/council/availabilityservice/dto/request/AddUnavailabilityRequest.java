package com.council.availabilityservice.dto.request;

import com.council.availabilityservice.model.UnavailabilityReason;
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
    private UnavailabilityReason reason;
}
