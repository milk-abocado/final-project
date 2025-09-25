package com.example.finalproject.domain.files.service;

import com.example.finalproject.domain.files.entity.Image;
import com.example.finalproject.domain.files.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ImageAppService {

    private final PresignedS3Service presignedS3Service;
    private final ImageRepository imageRepository;

    @Value("${aws.s3.base-dir:user-uploads}")
    private String baseDir;

    // 1) presign: 키 생성 + PUT URL 발급 (DB 저장 X)
    public Map<String, String> presignFor(String refType, Long refId,
                                          String originalName, String contentType, Long userId) {
        String safeName = (originalName == null) ? "noname" : originalName.replaceAll("\\s+", "_");
        String key = baseDir + "/user-" + userId + "/" + refType.toLowerCase() + "-" + refId + "/" + UUID.randomUUID() + "_" + safeName;
        String url = presignedS3Service.presignedPutUrl(key, contentType, Duration.ofMinutes(5));
        return Map.of("key", key, "uploadUrl", url);
    }

    // 2) confirm: 업로드 완료 후 DB에 저장
    @Transactional
    public Image confirm(String key, String refType, Long refId,
                         String purpose, String originalName, String contentType,
                         Long size, Long userId) {
        Image img = new Image();
        img.setKey(key);
        img.setRefType(refType.toUpperCase());
        img.setRefId(refId);
        img.setPurpose(purpose);
        img.setOriginalName(originalName);
        img.setContentType(contentType);
        img.setSize(size == null ? 0L : size);
        img.setOwnerUserId(userId);
        return imageRepository.save(img);
    }

    @Transactional(readOnly = true)
    public List<Image> list(String refType, Long refId) {
        return imageRepository.findByRefTypeAndRefId(refType.toUpperCase(), refId);
    }
}