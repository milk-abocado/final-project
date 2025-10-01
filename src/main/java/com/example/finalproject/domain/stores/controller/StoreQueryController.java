package com.example.finalproject.domain.stores.controller;

import com.example.finalproject.domain.stores.category.StoreCategory;
import com.example.finalproject.domain.stores.dto.response.StoreDetailResponse;
import com.example.finalproject.domain.stores.dto.response.StoreListItemResponse;
import com.example.finalproject.domain.stores.exception.StoresApiException;
import com.example.finalproject.domain.stores.exception.StoresErrorCode;
import com.example.finalproject.domain.stores.service.StoreQueryService;
import com.example.finalproject.domain.users.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
public class StoreQueryController {

    private final StoreQueryService storeQueryService;

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
                throw new StoresApiException(StoresErrorCode.BAD_REQUEST, "잘못된 카테고리: " + categoryParam);
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

    /** 가게 단건 상세 조회 (오너 전용)
     * - 현재 사용자 Role이 OWNER가 아닌 경우 403 Forbidden 반환
     */
    @GetMapping("/owners/stores/{storeId}")
    public ResponseEntity<StoreDetailResponse> getOneForOwner(@PathVariable Long storeId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        // 권한이 여러 개일 경우, 첫 번째 권한을 사용
        String roleString = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse(null);

        // UserRole로 변환하여 권한 확인
        UserRole currentRole = UserRole.valueOf(Objects.requireNonNull(roleString).replace("ROLE_", "")); // "ROLE_" 부분 제거 후 변환

        // OWNER 권한 확인
        if (currentRole != UserRole.OWNER) {
            throw new StoresApiException(StoresErrorCode.FORBIDDEN, "오너용 가게 단건 조회는 OWNER만 가능합니다.");
        }

        return ResponseEntity.ok(storeQueryService.getOneForOwner(storeId));
    }

}
