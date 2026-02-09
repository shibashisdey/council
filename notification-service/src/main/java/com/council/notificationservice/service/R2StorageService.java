package com.council.notificationservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;

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

        s3Client.putObject(request, RequestBody.fromBytes(content.getBytes(StandardCharsets.UTF_8)));
        return publicBaseUrl + "/" + objectKey;
    }
}
