package com.example.finalproject.domain.stores.controller;

import com.example.finalproject.domain.stores.dto.request.StoreNoticeRequest;
import com.example.finalproject.domain.stores.dto.response.ApiMessageResponse;
import com.example.finalproject.domain.stores.dto.response.StoreNoticeResponse;
import com.example.finalproject.domain.stores.exception.StoresApiException;
import com.example.finalproject.domain.stores.exception.StoresErrorCode;
import com.example.finalproject.domain.stores.service.StoreNoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
     * 가게 소유자 확인 메소드
     * - 인증된 사용자의 이메일을 이용하여, 해당 사용자가 가게 소유자인지 확인
     * @param storeId 가게 ID
     * @return 소유자 여부 (true: 소유자, false: 소유자가 아님)
     */
    private boolean isOwner(Long storeId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = authentication.getName();           // 인증된 사용자 이메일 가져오기
        return storeNoticeService.isOwner(storeId, currentEmail); // 이메일로 소유자 확인
    }

    /**
     * 가게 공지 등록 API
     * POST /owners/stores/{storeId}/notices
     */
    @PostMapping
    public ResponseEntity<StoreNoticeResponse> create(
            @PathVariable Long storeId,
            @Valid @RequestBody StoreNoticeRequest req
    ) {
        // 소유자가 아닐 경우, 공지 등록을 허용하지 않음
        if (!isOwner(storeId)) {
            throw new StoresApiException(StoresErrorCode.FORBIDDEN, "공지 등록은 OWNER만 가능합니다.");
        }

        // 공지 등록 서비스 호출
        StoreNoticeResponse res = storeNoticeService.create(storeId, req);

        // 공지가 성공적으로 등록되었음을 알리고 응답 반환
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
        // 공지 목록 조회 서비스 호출
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
        // 소유자가 아닐 경우, 공지 수정 허용하지 않음
        if (!isOwner(storeId)) {
            throw new StoresApiException(StoresErrorCode.FORBIDDEN, "공지 수정은 OWNER만 가능합니다.");
        }
        // 공지 수정 서비스 호출
        return ResponseEntity.ok(storeNoticeService.update(storeId, req));
    }

    /**
     * 가게 공지 삭제 API
     * DELETE /owners/stores/{storeId}/notices
     */
    @DeleteMapping
    public ResponseEntity<ApiMessageResponse> delete(@PathVariable Long storeId) {
        // 소유자가 아닐 경우, 공지 삭제 허용하지 않음
        if (!isOwner(storeId)) {
            throw new StoresApiException(StoresErrorCode.FORBIDDEN, "공지 삭제는 OWNER만 가능합니다.");
        }

        // 공지 삭제 서비스 호출
        storeNoticeService.delete(storeId);

        // 공지가 삭제되었음을 알리는 응답 반환
        return ResponseEntity.ok(new ApiMessageResponse("공지가 삭제되었습니다.", LocalDateTime.now()));
    }
}
