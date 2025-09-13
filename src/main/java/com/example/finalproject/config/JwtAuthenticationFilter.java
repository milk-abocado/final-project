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
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = TokenProvider.stripBearer(authHeader);

        if (StringUtils.hasText(token)) {
            if (tokenProvider.validateAccessToken(token)) {
                try {
                    Authentication authentication = tokenProvider.getAuthenticationFromAccess(token);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    if (log.isDebugEnabled()) {
                        log.debug("JWT authenticated: principal={} uri={}",
                                authentication.getPrincipal(), uri);
                    }
                } catch (Exception e) {
                    log.warn("Failed to build Authentication from JWT: {}", e.getMessage(), e);
                    SecurityContextHolder.clearContext();
                }
            } else {
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }
}