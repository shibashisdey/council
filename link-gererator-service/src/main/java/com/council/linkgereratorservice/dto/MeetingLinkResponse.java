package com.council.linkgereratorservice.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class MeetingLinkResponse {

    private Long appointmentId;
    private Long clientId;
    private Long counselorId;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String meetingLink;
}
