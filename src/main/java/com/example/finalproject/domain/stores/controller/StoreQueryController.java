package com.example.finalproject.domain.stores.controller;

import com.example.finalproject.domain.stores.auth.Role;
import com.example.finalproject.domain.stores.auth.SecurityUtil;
import com.example.finalproject.domain.stores.category.StoreCategory;
import com.example.finalproject.domain.stores.dto.response.StoreDetailResponse;
import com.example.finalproject.domain.stores.dto.response.StoreListItemResponse;
import com.example.finalproject.domain.stores.exception.ApiException;
import com.example.finalproject.domain.stores.exception.ErrorCode;
import com.example.finalproject.domain.stores.service.StoreQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StoreQueryController {

    private final StoreQueryService storeQueryService;
    private final SecurityUtil security;

    /**
     * 가게 검색
     * -------------------------------------------------
     * - address 가 있으면: 서버가 지오코딩하여 lat/lng 설정 후 반경 검색
     * - lat/lng/radiusKm 직접 주면: 그대로 반경 검색
     * - keyword: 가게명/주소 등 키워드 검색
     * - category: 한글/영문 모두 허용 (예: "분식" 또는 "SNACK")
     */
    @GetMapping("/stores")
    public ResponseEntity<Page<StoreListItemResponse>> search(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false, defaultValue = "3") Double radiusKm,
            @RequestParam(required = false, name = "category") String categoryParam, // ← String으로 받음
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var pageable = PageRequest.of(page, size);

        // 한글/영문 모두 매핑 (예: "분식" → SNACK, "SNACK" → SNACK)
        StoreCategory category = null;
        if (categoryParam != null && !categoryParam.isBlank()) {
            try {
                category = StoreCategory.from(categoryParam.trim());
            } catch (IllegalArgumentException e) {
                // 잘못된 카테고리 값이면 400 반환
                throw new ApiException(ErrorCode.BAD_REQUEST, "잘못된 카테고리: " + categoryParam);
            }
        }

        return ResponseEntity.ok(
                storeQueryService.search(keyword, address, lat, lng, radiusKm, category, pageable)
        );
    }

    /** 가게 단건 상세 조회 (일반 사용자용) */
    @GetMapping("/stores/{storeId}")
    public ResponseEntity<StoreDetailResponse> getOne(@PathVariable Long storeId) {
        return ResponseEntity.ok(storeQueryService.getOne(storeId));
    }

    /**
     * 가게 단건 상세 조회 (오너 전용)
     * - 현재 사용자 Role 이 OWNER 가 아닌 경우 403 Forbidden 반환
     */
    @GetMapping("/owners/stores/{storeId}")
    public ResponseEntity<StoreDetailResponse> getOneForOwner(@PathVariable Long storeId) {
        if (security.currentRole() != Role.OWNER) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(storeQueryService.getOneForOwner(storeId));
    }
}
