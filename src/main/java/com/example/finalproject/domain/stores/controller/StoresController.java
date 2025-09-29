package com.example.finalproject.domain.stores.controller;

import com.example.finalproject.domain.stores.dto.request.StoresRequest;
import com.example.finalproject.domain.stores.dto.response.StoresResponse;
import com.example.finalproject.domain.stores.exception.StoresApiException;
import com.example.finalproject.domain.stores.exception.StoresErrorCode;
import com.example.finalproject.domain.stores.service.StoreLifecycleService;
import com.example.finalproject.domain.stores.service.StoresService;
import com.example.finalproject.domain.users.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/owners/stores") // 가게 생성 등 OWNER 전용 엔드포인트
@RequiredArgsConstructor
public class StoresController {

    private final StoresService storesService;
    private final StoreLifecycleService lifecycleService;

    /**
     * 가게 생성 API
     * - 로그인된 사용자만 가게 생성 가능
     */
    @PostMapping
    public ResponseEntity<StoresResponse> create(@RequestBody @Valid StoresRequest req) {
        // 로그인된 유저 정보 확인
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        // 권한이 여러 개일 경우, 첫 번째 권한을 사용
        String roleString = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse(null);

        UserRole currentRole = UserRole.valueOf(Objects.requireNonNull(roleString).replace("ROLE_", "")); // "ROLE_" 부분 제거 후 변환

        // OWNER 권한 확인
        if (currentRole != UserRole.OWNER) {
            throw new StoresApiException(StoresErrorCode.FORBIDDEN, "가게 생성은 OWNER만 가능합니다.");
        }

        // 가게 생성
        StoresResponse res = storesService.create(req);

        URI location = URI.create("/owners/stores/" + res.getId());
        return ResponseEntity.created(location).body(res);
    }


    /**
     * 가게 수정 API
     * - 로그인된 사용자만 가게 수정 가능
     */
    @PutMapping("/{storeId}")
    public ResponseEntity<StoresResponse> update(
            @PathVariable Long storeId,
            @RequestBody @Valid StoresRequest req
    ) {
        // 로그인된 유저 정보 확인
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
            throw new StoresApiException(StoresErrorCode.FORBIDDEN, "가게 수정은 OWNER만 가능합니다.");
        }

        // 가게 수정
        StoresResponse res = storesService.update(storeId, req);
        return ResponseEntity.ok(res);
    }

    /** 가게 폐업(논리 삭제) */
    @PostMapping("/{storeId}/retire")
    public ResponseEntity<Map<String, String>> retire(@PathVariable Long storeId) {
        // 로그인된 유저 정보 확인
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
            throw new StoresApiException(StoresErrorCode.FORBIDDEN, "가게 폐업은 OWNER만 가능합니다.");
        }

        // 가게 폐업 처리
        String storeName = lifecycleService.retire(storeId);

        // 따옴표 없이 자연스러운 메시지로 반환
        Map<String, String> body = Map.of("message", storeName + " 가게가 폐업되었습니다.");
        return ResponseEntity.ok(body);
    }
}
