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

    // 완전 공개 (모든 HTTP 메서드 허용)
    private static final String[] PUBLIC_ANY = {
            "/auth/**",          // 로그인/회원가입/비번재설정 등
            "/oauth/**"
    };

    // 읽기(GET)만 공개할 리소스
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

    private static final String[] PUBLIC_POST = {
            "/searches/**",          // 바디로 검색하고 싶을 때
             "/image/**",          // 예: 이미지 업로드를 공개하고 싶다면(보안 주의)
             "/slack/**",           // 필요 시 추가
             "/files/**",
             "/s3/upload",
            "/images/*/*/presign",
            "/images/*/*/confirm"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ANY).permitAll()                  // 모든 메서드 공개
                        .requestMatchers(HttpMethod.GET,  PUBLIC_GET).permitAll() // GET만 공개
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST).permitAll()//  POST 공개
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()   // CORS preflight
                        .anyRequest().authenticated()                             // 나머지 인증 필요
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
                );

        // JWT 필터 추가
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
