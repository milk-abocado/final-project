package com.example.finalproject.domain.files.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class S3Service {

    private final AmazonS3 s3;

    @Value("${aws.s3.bucket}")
    private String bucket;

    public S3Service(AmazonS3 s3) { this.s3 = s3; }

    public String uploadImage(MultipartFile image) throws IOException {
        String key = "user-uploads/" + UUID.randomUUID() + "_" + image.getOriginalFilename();

        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentType(image.getContentType());
        meta.setContentLength(image.getSize());

        PutObjectRequest put = new PutObjectRequest(bucket, key, image.getInputStream(), meta);
        s3.putObject(put);

        return key;
    }
}