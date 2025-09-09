package com.example.finalproject.domain.stores.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 테스트 환경 전용 인증 필터
 * -------------------------------------------------
 * - Postman 등에서 Header 로 임의의 사용자/권한을 주입할 수 있게 해 줌
 * - 사용법 (Postman Headers 예시):
 *   X-User-Id: 1
 *   X-Role: OWNER   // USER, ADMIN 도 가능
 */
@Component
@Order(1) // 가장 앞단에서 실행되도록 우선순위 지정
@RequiredArgsConstructor
public class TestAuthFilter extends OncePerRequestFilter {

    private final SecurityUtil security;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            // 요청 헤더에서 사용자 ID와 권한(Role) 추출
            String uidHeader = request.getHeader("X-User-Id");
            String roleHeader = request.getHeader("X-Role");

            if (uidHeader != null && roleHeader != null) {
                // ID를 Long으로 변환하고, Role Enum 값으로 변환
                Long uid = Long.parseLong(uidHeader);
                Role role = Role.valueOf(roleHeader.toUpperCase()); // OWNER/USER/ADMIN
                // SecurityUtil 에 현재 사용자 정보 설정
                security.set(uid, role);
            }
            // 다음 필터로 요청 전달
            filterChain.doFilter(request, response);
        } finally {
            security.clear(); // 요청이 끝날 때 항상 비우기
        }
    }
}
