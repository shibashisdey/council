package com.council.emailservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RenderedEmail {
    private String to;
    private String subject;
    private String body;
}
