package com.example.finalproject.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;
    private final SessionIndexService sidStore; // 현재 세션 sid 조회용(읽기 전용)

    // 인증을 건너뛸 공개 경로들 (SecurityConfig의 permitAll과 일치시켜 주세요)
    private static final List<String> SKIP_PATHS = List.of(
            "/auth/signup",
            "/auth/login",
            "/oauth/**",
            "/public/**",
            "/auth/password/reset/code",
            "/auth/password/reset/verify",
            "/auth/password/reset/confirm"
    );
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // (선택) CORS preflight 우회
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        for (String p : SKIP_PATHS) {
            if (PATH_MATCHER.match(p, uri)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token  = TokenProvider.stripBearer(header);

        // 헤더 유무/원문 peek
        String headPeek = header == null ? "(null)"
                : (header.length() > 25 ? header.substring(0, 25) + "..." : header);
        log.info("[JWT] hdr? {} raw='{}' uri={}", header != null, headPeek, request.getRequestURI());

        if (header != null && token == null) {
            log.info("[JWT] Authorization 존재하지만 Bearer 형식 아님. uri={}", request.getRequestURI());
        }

        try {
            if (token != null) {
                String peek = token.length() > 10
                        ? token.substring(0, 10) + "..." + token.substring(token.length() - 10)
                        : token;
                log.info("[JWT] validate start: uri={}, token.peek={}", request.getRequestURI(), peek);

                boolean ok = tokenProvider.validateAccessToken(token); // 원인별 상세 로그는 TokenProvider에서 기록
                log.info("[JWT] validate result: {}", ok);

                if (ok) {
                    Authentication authentication = tokenProvider.getAuthenticationFromAccess(token);

                    // --- "중복 로그인(sid 변경)" 여부 실시간 검증 ---
                    @SuppressWarnings("unchecked")
                    Map<String, Object> details = (Map<String, Object>) authentication.getDetails();

                    Long uid = null;
                    if (details != null) {
                        try {
                            Object uidObj = details.get("uid");
                            if (uidObj instanceof Number n) uid = n.longValue();
                            else if (uidObj != null)        uid = Long.valueOf(uidObj.toString());
                        } catch (Exception ignored) {}
                    }

                    String tokenSid = (details != null && details.get("sid") != null)
                            ? Objects.toString(details.get("sid"))
                            : null;

                    String currentSid = (uid != null) ? sidStore.get(uid) : null; // SessionIndexService#get

                    if (uid != null && tokenSid != null && currentSid != null && !tokenSid.equals(currentSid)) {
                        // 다른 기기에서 새 로그인 → 현재 토큰 즉시 차단
                        log.warn("[JWT] sid mismatch → kick. uid={}, tokenSid={}, currentSid={}", uid, tokenSid, currentSid);
                        SecurityContextHolder.clearContext();

                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json;charset=" + StandardCharsets.UTF_8);
                        response.getWriter().write(
                                "{\"error\":\"invalid_session\",\"message\":\"다른 기기에서 로그인되어 세션이 만료되었습니다.\"}"
                        );
                        return; // 바로 종료
                    }
                    // ----------------------------------------------------------

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.info("[JWT] OK -> name={}, details={}", authentication.getName(), details);
                }
            }
        } catch (Exception e) {
            log.warn("[JWT] invalid ({}): {}", request.getRequestURI(), e.getMessage());
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }
}
