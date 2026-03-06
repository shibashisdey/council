package com.council.notificationservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SessionNoteShareRequest {
    private Long noteId;
    private Long appointmentId;
    private String summary;
    private String observations;
    private String recommendations;
}
