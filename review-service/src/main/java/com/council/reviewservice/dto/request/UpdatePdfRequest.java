package com.council.reviewservice.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePdfRequest {
    private String pdfObjectKey;
    private String pdfUrl;
}
