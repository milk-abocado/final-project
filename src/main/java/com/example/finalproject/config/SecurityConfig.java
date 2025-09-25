package com.example.finalproject.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // 완전 공개 (모든 HTTP 메서드 허용) - 필요한 것만 한정! (/auth/** 전체 공개 금지)
    private static final String[] PUBLIC_ANY = {
            "/auth/signup",
            "/auth/login",
            "/oauth/**",
            "/public/**",
            "/auth/password/reset/code",
            "/auth/password/reset/verify",
            "/auth/password/reset/confirm"
    };

    // 읽기(GET)만 공개
    private static final String[] PUBLIC_GET = {
            "/searches/**",
            "/stores/**",
            "/menus/**",
            "/reviews/**",
            "/points/**",
            "/coupons/**",
            "/image/**",
            "/users/**",
            "/slack/**",
            "/carts/**",
            "/orders/**",
            "/files/**",
            "/images/*/*"
    };

    // POST만 공개 (정책에 맞게 유지/조정)
    private static final String[] PUBLIC_POST = {
            "/searches/**",
            "/image/**",
            "/slack/**",
            "/files/**",
            "/s3/upload",
            "/images/*/*/presign",
            "/images/*/*/confirm"
    };


     //private static final String[] PUBLIC_PATCH = {
     //   "/users/*/profile"
     //};

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 완전 공개
                        .requestMatchers(PUBLIC_ANY).permitAll()

                        // GET 공개
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET).permitAll()

                        // POST 공개
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST).permitAll()

                        // PATCH 공개 (현재는 주석: 프로필 수정은 인증 필요 권장)
                        //.requestMatchers(HttpMethod.PATCH, PUBLIC_PATCH).permitAll()

                        // 에러/프리플라이트
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 나머지 모두 인증 필요
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            res.setHeader("WWW-Authenticate", "Bearer");
                            res.setStatus(401);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"Authentication required\"}");
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            res.setStatus(403);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"error\":\"forbidden\",\"message\":\"Access denied\"}");
                        })
                )
                // UsernamePasswordAuthenticationFilter 이전에 JWT 필터 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
