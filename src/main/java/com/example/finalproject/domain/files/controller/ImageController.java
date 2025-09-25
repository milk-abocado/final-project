package com.example.finalproject.domain.files.controller;

import com.example.finalproject.domain.files.entity.Image;
import com.example.finalproject.domain.files.model.RefType;
import com.example.finalproject.domain.files.service.ImageAppService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
@Validated
public class ImageController{

    private final ImageAppService imageAppService;

    // 1) Presign: JSON 바디로 filename / contentType 받기
    @PostMapping(
            value = "/{refType}/{refId}/presign",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String,String>> presignJson(
            @PathVariable RefType refType,
            @PathVariable Long refId,
            @RequestBody PresignReq req
    ) {
        var res = imageAppService.presignFor(refType.name(), refId, req.getFilename(), req.getContentType(), 0L);
        return ResponseEntity.ok(res);
    }

    @Getter @Setter
    public static class PresignReq {
        @NotBlank
        private String filename;

        // 허용 타입 화이트리스트(예: png/jpeg/webp). 필요 시 패턴 수정
        @NotBlank
        @Pattern(regexp = "image/(png|jpeg|jpg|webp)")
        private String contentType;
    }

    // 2) 업로드 확정(메타 저장): JSON 바디로 key/메타 받기
    @PostMapping(
            value = "/{refType}/{refId}/confirm",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> confirmJson(
            @PathVariable RefType refType,
            @PathVariable Long refId,
            @RequestBody @Validated ConfirmReq body,
            @RequestHeader(name = "X-USER-ID", required = false) Long userId
    ) {
        if (userId == null) userId = 0L; // 데모용
        Image img = imageAppService.confirm(
                body.getKey(),
                refType.name(),
                refId,
                body.getPurpose(),
                body.getOriginalName(),
                body.getContentType() == null ? "application/octet-stream" : body.getContentType(),
                body.getSize(),
                userId
        );
        return ResponseEntity.ok(Map.of(
                "id", img.getId(),
                "key", img.getKey(),
                "refType", img.getRefType(),
                "refId", img.getRefId(),
                "purpose", img.getPurpose()
        ));
    }

    @Getter @Setter
    public static class ConfirmReq {
        @NotBlank
        private String key;                 // presign 응답의 key 그대로
        private String originalName;        // 선택
        private String contentType;         // 선택 (예: image/png)
        private Long   size;                // 선택 (바이트)
        private String purpose;             // 선택 (예: THUMB, GALLERY)
    }
}
