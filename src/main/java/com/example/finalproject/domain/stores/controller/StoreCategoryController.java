package com.example.finalproject.domain.stores.controller;

import com.example.finalproject.domain.stores.category.StoreCategory;
import com.example.finalproject.domain.stores.dto.request.StoreCategoriesRequest;
import com.example.finalproject.domain.stores.dto.response.StoreCategoriesDeleteResponse;
import com.example.finalproject.domain.stores.dto.response.StoreCategoriesResponse;
import com.example.finalproject.domain.stores.service.StoreCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/owners/stores/{storeId}/categories")
public class StoreCategoryController {

    private final StoreCategoryService service;

    /**
     * 등록(최초 1회) — 이미 있으면 409
     * - 최초로 카테고리를 등록할 때 사용
     * - 만약 이미 등록된 카테고리가 있다면 409 CONFLICT 오류 발생
     *
     * @param storeId 가게 ID
     * @param req 요청 카테고리 정보
     * @return 등록된 카테고리 목록
     */
    @PostMapping
    public ResponseEntity<StoreCategoriesResponse> create(
            @PathVariable Long storeId,
            @Valid @RequestBody StoreCategoriesRequest req
    ) {
        // 요청된 카테고리를 등록하고, 등록된 카테고리 목록을 반환
        return ResponseEntity.status(201).body(service.create(storeId, req));
    }

    /**
     * 수정(전체 치환) — Body에 1~2개
     * - 기존 카테고리들을 삭제하고 새로운 카테고리 목록으로 교체
     * - 카테고리는 최대 2개까지만 설정 가능
     *
     * @param storeId 가게 ID
     * @param req 요청된 카테고리 정보
     * @return 수정된 카테고리 목록
     */
    @PutMapping
    public ResponseEntity<StoreCategoriesResponse> update(
            @PathVariable Long storeId,
            @Valid @RequestBody StoreCategoriesRequest req
    ) {
        // 요청된 카테고리로 기존 카테고리를 업데이트하고, 업데이트된 목록 반환
        return ResponseEntity.ok(service.update(storeId, req));
    }

    /**
     * 단일 삭제 — PathVariable 한글/영문 모두 허용
     * - 특정 카테고리만 삭제
     *
     * @param storeId 가게 ID
     * @param category 삭제할 카테고리
     * @return 삭제된 후 남은 카테고리 목록
     */
    @DeleteMapping("/{category}")
    public ResponseEntity<StoreCategoriesDeleteResponse> removeOne(
            @PathVariable Long storeId,
            @PathVariable String category // ← 카테고리를 String으로 받아서 매핑
    ) {
        // 한글/영문 모두 허용하는 카테고리 값 매핑
        StoreCategory enumCategory = StoreCategory.fromPath(category); // ← 한글/영문 매핑
        var res = service.removeOne(storeId, enumCategory);
        return ResponseEntity.ok(res);
    }

    /**
     * 전체 삭제
     * - 가게의 모든 카테고리 삭제
     *
     * @param storeId 가게 ID
     * @return 삭제된 후 남은 카테고리 목록 (보통 빈 리스트)
     */
    @DeleteMapping
    public ResponseEntity<StoreCategoriesDeleteResponse> removeAll(@PathVariable Long storeId) {
        // 가게의 모든 카테고리를 삭제하고, 상태 반환
        var res = service.removeAll(storeId);
        return ResponseEntity.ok(res);
    }

    /**
     * 조회
     * - 가게의 카테고리 목록 조회
     *
     * @param storeId 가게 ID
     * @return 현재 등록된 카테고리 목록
     */
    @GetMapping
    public ResponseEntity<StoreCategoriesResponse> get(@PathVariable Long storeId) {
        // 가게의 카테고리 목록 조회 후 반환
        return ResponseEntity.ok(service.get(storeId));
    }
}
