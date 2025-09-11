package com.example.finalproject.domain.stores.controller;

import com.example.finalproject.domain.stores.dto.request.StoreNoticeRequest;
import com.example.finalproject.domain.stores.dto.response.ApiMessageResponse;
import com.example.finalproject.domain.stores.dto.response.StoreNoticeResponse;
import com.example.finalproject.domain.stores.service.StoreNoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/owners/stores/{storeId}/notices")
public class StoreNoticeController {

    private final StoreNoticeService storeNoticeService;

    /**
     * 가게 공지 등록 API
     * POST /owners/stores/{storeId}/notices
     */
    @PostMapping
    public ResponseEntity<StoreNoticeResponse> create(
            @PathVariable Long storeId,
            @Valid @RequestBody StoreNoticeRequest req
    ) {
        StoreNoticeResponse res = storeNoticeService.create(storeId, req);
        return ResponseEntity
                .created(URI.create("/owners/stores/" + storeId + "/notices"))
                .body(res);
    }

    /**
     * 가게 공지 목록 조회 API
     * GET /owners/stores/{storeId}/notices?activeOnly=true|false
     */
    @GetMapping
    public ResponseEntity<List<StoreNoticeResponse>> list(
            @PathVariable Long storeId,
            @RequestParam(defaultValue = "false") boolean activeOnly
    ) {
        return ResponseEntity.ok(storeNoticeService.findAll(storeId, activeOnly));
    }

    /**
     * 가게 공지 수정 API
     * PATCH /owners/stores/{storeId}/notices
     */
    @PatchMapping
    public ResponseEntity<StoreNoticeResponse> update(
            @PathVariable Long storeId,
            @Valid @RequestBody StoreNoticeRequest req
    ) {
        return ResponseEntity.ok(storeNoticeService.update(storeId, req));
    }

    /**
     * 가게 공지 삭제 API
     * DELETE /owners/stores/{storeId}/notices
     */
    @DeleteMapping
    public ResponseEntity<ApiMessageResponse> delete(@PathVariable Long storeId) {
        storeNoticeService.delete(storeId);
        return ResponseEntity.ok(new ApiMessageResponse("공지가 삭제되었습니다.", LocalDateTime.now()));
    }
}
