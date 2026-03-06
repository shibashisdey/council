package com.council.reviewservice.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotifySessionNoteRequest {
    private Long noteId;
    private Long appointmentId;
    private String summary;
    private String observations;
    private String recommendations;
}
