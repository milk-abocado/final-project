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
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;

    // 인증을 건너뛸 공개 경로들 (permitAll 과 일치시키기)
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
        for (String p : SKIP_PATHS) {
            if (PATH_MATCHER.match(p, uri)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = TokenProvider.stripBearer(header);

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

                boolean ok = tokenProvider.validateAccessToken(token); // ← 여기서 원인별 로그가 남습니다
                log.info("[JWT] validate result: {}", ok);

                if (ok) {
                    Authentication authentication = tokenProvider.getAuthenticationFromAccess(token);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.info("[JWT] OK -> name={}, details={}", authentication.getName(), authentication.getDetails());
                }
            }
        } catch (Exception e) {
            log.warn("[JWT] invalid ({}): {}", request.getRequestURI(), e.getMessage());
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }}