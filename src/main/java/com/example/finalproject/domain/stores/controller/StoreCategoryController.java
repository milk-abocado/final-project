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

    /** 등록(최초 1회) — 이미 있으면 409 */
    @PostMapping
    public ResponseEntity<StoreCategoriesResponse> create(
            @PathVariable Long storeId,
            @Valid @RequestBody StoreCategoriesRequest req
    ) {
        // RequestBody는 StoreCategory.from(@JsonCreator)로 한글/영문 모두 처리됨
        return ResponseEntity.status(201).body(service.create(storeId, req));
    }

    /** 수정(전체 치환) — Body에 1~2개 */
    @PutMapping
    public ResponseEntity<StoreCategoriesResponse> update(
            @PathVariable Long storeId,
            @Valid @RequestBody StoreCategoriesRequest req
    ) {
        // RequestBody는 StoreCategory.from(@JsonCreator)로 한글/영문 모두 처리됨
        return ResponseEntity.ok(service.update(storeId, req));
    }

    /** 단일 삭제 — PathVariable 한글/영문 모두 허용 */
    @DeleteMapping("/{category}")
    public ResponseEntity<StoreCategoriesDeleteResponse> removeOne(
            @PathVariable Long storeId,
            @PathVariable String category // ← String으로 받고
    ) {
        StoreCategory enumCategory = StoreCategory.fromPath(category); // ← 한글/영문 매핑
        var res = service.removeOne(storeId, enumCategory);
        return ResponseEntity.ok(res);
    }

    /** 전체 삭제 */
    @DeleteMapping
    public ResponseEntity<StoreCategoriesDeleteResponse> removeAll(@PathVariable Long storeId) {
        var res = service.removeAll(storeId);
        return ResponseEntity.ok(res);
    }

    /** 조회(누구나) */
    @GetMapping
    public ResponseEntity<StoreCategoriesResponse> get(@PathVariable Long storeId) {
        return ResponseEntity.ok(service.get(storeId));
    }
}
