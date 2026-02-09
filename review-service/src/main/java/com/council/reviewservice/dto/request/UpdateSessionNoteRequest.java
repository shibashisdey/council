package com.council.reviewservice.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateSessionNoteRequest {
    private String summary;
    private String observations;
    private String recommendations;
    private String privateNotes;
}
