package com.council.reviewservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class SessionNoteClientResponse {
    private Long id;
    private Long appointmentId;
    private Long counselorId;
    private LocalDate sessionDate;
    private String summary;
    private String observations;
    private String recommendations;
    private String pdfUrl;
}
