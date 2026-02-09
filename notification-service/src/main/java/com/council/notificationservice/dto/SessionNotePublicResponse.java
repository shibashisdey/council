package com.council.notificationservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class SessionNotePublicResponse {
    private Long id;
    private Long appointmentId;
    private Long userId;
    private Long counselorId;
    private LocalDate sessionDate;
    private String summary;
    private String observations;
    private String recommendations;
    private String pdfObjectKey;
    private String pdfUrl;
}
