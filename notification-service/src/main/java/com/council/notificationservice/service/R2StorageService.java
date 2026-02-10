package com.council.notificationservice.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class R2StorageService {

    private final S3Client s3Client;
    private final String bucket;
    private final String publicBaseUrl;

    public R2StorageService(
            S3Client s3Client,
            @Value("${r2.bucket}") String bucket,
            @Value("${r2.public-base-url}") String publicBaseUrl
    ) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl;
    }

    public String uploadPdfPlaceholder(String objectKey, String content) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType("application/pdf")
                .build();

        byte[] pdfBytes = buildPdfBytes(content);
        s3Client.putObject(request, RequestBody.fromBytes(pdfBytes));
        return publicBaseUrl + "/" + objectKey;
    }

    private byte[] buildPdfBytes(String content) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 750);

                float leading = 14f;
                for (String line : wrapLines(content)) {
                    contentStream.showText(line);
                    contentStream.newLineAtOffset(0, -leading);
                }

                contentStream.endText();
            }

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                document.save(out);
                return out.toByteArray();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate PDF content", e);
        }
    }

    private List<String> wrapLines(String content) {
        List<String> lines = new ArrayList<>();
        if (content == null || content.isBlank()) {
            lines.add("Session note");
            return lines;
        }

        String[] rawLines = content.split("\\r?\\n");
        int maxChars = 90;
        for (String rawLine : rawLines) {
            if (rawLine.length() <= maxChars) {
                lines.add(rawLine);
                continue;
            }
            int start = 0;
            while (start < rawLine.length()) {
                int end = Math.min(start + maxChars, rawLine.length());
                lines.add(rawLine.substring(start, end));
                start = end;
            }
        }
        return lines;
    }
}
