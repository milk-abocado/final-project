package com.example.finalproject.domain.stores.controller;

import com.example.finalproject.domain.stores.auth.Role;
import com.example.finalproject.domain.stores.auth.SecurityUtil;
import com.example.finalproject.domain.stores.dto.request.StoresRequest;
import com.example.finalproject.domain.stores.dto.response.StoresResponse;
import com.example.finalproject.domain.stores.service.StoreLifecycleService;
import com.example.finalproject.domain.stores.service.StoresService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/owners/stores") // 가게 생성 등 OWNER 전용 엔드포인트
@RequiredArgsConstructor
public class StoresController {

    private final StoresService storesService;
    private final StoreLifecycleService lifecycleService;
    private final SecurityUtil security;

    /**
     * 가게 생성 API
     * - 현재는 테스트 편의를 위해 강제 로그인 설정 사용
     */
    @PostMapping
    public ResponseEntity<StoresResponse> create(@RequestBody @Valid StoresRequest req) {
        // 🔹 (테스트 전용) 강제 로그인
        security.set(1L, Role.OWNER);

        StoresResponse res = storesService.create(req);

        URI location = URI.create("/owners/stores/" + res.getId());
        return ResponseEntity.created(location).body(res);
    }

    /**
     * 가게 수정 API
     */
    @PutMapping("/{storeId}")
    public ResponseEntity<StoresResponse> update(
            @PathVariable Long storeId,
            @RequestBody @Valid StoresRequest req
    ) {
        security.set(1L, Role.OWNER);

        StoresResponse res = storesService.update(storeId, req);
        return ResponseEntity.ok(res);
    }

    /** 가게 폐업(논리 삭제) */
    @PostMapping("/{storeId}/retire")
    public ResponseEntity<Map<String, String>> retire(@PathVariable Long storeId) {
        security.set(1L, Role.OWNER); // 테스트 전용
        lifecycleService.retire(storeId);

        Map<String, String> body = Map.of("message", "폐업되었습니다.");
        return ResponseEntity.ok(body);
    }
}
