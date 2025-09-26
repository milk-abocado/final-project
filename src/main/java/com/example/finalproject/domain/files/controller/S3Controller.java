package com.example.finalproject.domain.files.controller;

import com.example.finalproject.domain.files.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/s3")
@RequiredArgsConstructor
public class S3Controller {

    private final S3Service s3Service;

    // 멀티파트 업로드 명시 (Postman: Body -> form-data, key: file, type: File)
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("{\"error\":\"bad_request\",\"message\":\"file part is missing or empty\"}");
        }
        try {
            String key = s3Service.uploadImage(file);
            return ResponseEntity.ok("{\"key\":\"" + key + "\"}");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("{\"error\":\"upload_failed\",\"message\":\"" + e.getMessage() + "\"}");
        }
    }
}
