package com.council.reviewservice.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateSessionNoteRequest {
    private Long appointmentId;
    private LocalDate sessionDate;
    private String summary;
    private String observations;
    private String recommendations;
    private String privateNotes;
}
