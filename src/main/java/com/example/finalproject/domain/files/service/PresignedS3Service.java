package com.example.finalproject.domain.files.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class PresignedS3Service {

    private final AmazonS3 s3;
    @Value("${aws.s3.bucket}") private String bucket;

    public PresignedS3Service(AmazonS3 s3) { this.s3 = s3; }

    public String presignedPutUrl(String key, String contentType, Duration ttl) {
        var expiration = Date.from(Instant.now().plus(ttl));
        var req = new com.amazonaws.services.s3.model.GeneratePresignedUrlRequest(bucket, key)
                .withMethod(HttpMethod.PUT)
                .withExpiration(expiration);
        // Content-Type 고정(클라이언트가 같은 타입으로 PUT해야 함)
        req.addRequestParameter("Content-Type", contentType);
        URL url = s3.generatePresignedUrl(req);
        return url.toString();
    }

    public String presignedGetUrl(String key, Duration ttl) {
        var expiration = Date.from(Instant.now().plus(ttl));
        var req = new com.amazonaws.services.s3.model.GeneratePresignedUrlRequest(bucket, key)
                .withMethod(HttpMethod.GET)
                .withExpiration(expiration);
        URL url = s3.generatePresignedUrl(req);
        return url.toString();
    }
}