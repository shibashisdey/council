package com.council.reviewservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class SessionNoteCounselorResponse {
    private Long id;
    private Long appointmentId;
    private Long userId;
    private Long counselorId;
    private LocalDate sessionDate;
    private String summary;
    private String observations;
    private String recommendations;
    private String privateNotes;
    private boolean sharedWithClient;
    private String pdfObjectKey;
    private String pdfUrl;
}
