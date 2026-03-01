package com.council.linkgereratorservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class UpdateMeetingLinkRequest {

    private Long clientId;
    private Long counselorId;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;
}
