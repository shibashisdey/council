package com.council.notificationservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePdfRequest {
    private String pdfObjectKey;
    private String pdfUrl;
}
