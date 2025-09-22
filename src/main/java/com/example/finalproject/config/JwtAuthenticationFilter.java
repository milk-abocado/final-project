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
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider; // 프로젝트의 실제 구현

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
                                    FilterChain chain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = TokenProvider.stripBearer(authHeader);

        try {
            if (StringUtils.hasText(token) && tokenProvider.validateAccessToken(token)) {
                Authentication authentication = tokenProvider.getAuthenticationFromAccess(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                if (log.isDebugEnabled()) {
                    log.debug("JWT authenticated: principal='{}' uri='{}'",
                            authentication.getPrincipal(), uri);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to build Authentication from JWT: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();

        }

        chain.doFilter(request, response);
    }
}
