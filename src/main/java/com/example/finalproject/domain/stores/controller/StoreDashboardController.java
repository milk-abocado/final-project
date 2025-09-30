package com.example.finalproject.domain.stores.controller;

import com.example.finalproject.domain.stores.dto.response.StoreDashboardResponse;
import com.example.finalproject.domain.stores.exception.StoresApiException;
import com.example.finalproject.domain.stores.exception.StoresErrorCode;
import com.example.finalproject.domain.stores.service.StoreDashboardService;
import com.example.finalproject.domain.users.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Objects;

@RestController
@RequestMapping("/owners/stores")
@RequiredArgsConstructor
public class StoreDashboardController {

    private final StoreDashboardService dashboardService;

    /**
     * 가게 대시보드 조회
     * --------------------------------------------------------
     * - URL: GET /owners/stores/{storeId}/dashboard
     * - 권한: OWNER만 접근 가능
     * - 요청 파라미터
     *   @param storeId 가게 ID (PathVariable)
     *   @param from    조회 시작일 (KST LocalDate)
     *   @param to      조회 종료일 (KST LocalDate)
     *   @param grain   집계 단위 (day | month), 기본값 day
     *   @param topN    인기 메뉴 조회 개수, 기본값 5
     * ///
     * 처리 단계:
     * (1) ROLE 확인 (Controller 레벨 1차 방어)
     *      - SecurityContextHolder → GrantedAuthority
     *      - ROLE_OWNER만 허용, 아니면 FORBIDDEN 예외 발생
     * (2) Service 호출
     *      - 내부에서 소유권 검증 및 집계 처리
     * (3) StoreDashboardResponse 응답 반환
     */
    @GetMapping("/{storeId}/dashboard")
    public ResponseEntity<StoreDashboardResponse> getDashboard(
            @PathVariable Long storeId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(defaultValue = "day") String grain,
            @RequestParam(name = "topN", defaultValue = "5") int topN
    ) {
        // (1) ROLE 확인 (OWNER만 허용)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String roleString = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse(null);
        UserRole currentRole = UserRole.valueOf(Objects.requireNonNull(roleString).replace("ROLE_", ""));
        if (currentRole != UserRole.OWNER) {
            throw new StoresApiException(StoresErrorCode.FORBIDDEN, "가게 대시보드는 OWNER만 조회 가능합니다.");
        }

        // (2) 서비스 호출 (내부에서 소유권 검증 + 집계)
        StoreDashboardResponse body = dashboardService.getDashboard(storeId, from, to, grain, topN);

        // (3) 응답 반환
        return ResponseEntity.ok(body);
    }
}
