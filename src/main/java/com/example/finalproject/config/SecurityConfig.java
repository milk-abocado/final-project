//package com.example.finalproject.config;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpMethod;
//import org.springframework.security.config.Customizer;
//import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//@Configuration
//@EnableWebSecurity
//@EnableMethodSecurity(prePostEnabled = true) // ← @PreAuthorize 사용하려면 필요
//public class SecurityConfig {
//    @Bean
//    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .csrf(csrf -> csrf.disable())
//                .cors(Customizer.withDefaults())
//                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                .authorizeHttpRequests(auth -> auth
//                        // 비로그인 허용 경로
//                        .requestMatchers(
//                                "/auth/**",
//                                "/oauth/**",          // OAuth 콜백/코드 교환 사용한다면 열어두기
//                                "/error",
//                                "/actuator/health"
//                        ).permitAll()
//                        // (예시) 공개 GET API가 있으면 선택적으로 열기
//                        .requestMatchers(HttpMethod.GET, "/stores/**", "/menus/**").permitAll()
//                        // 장바구니는 USER 권한 필요
//                        .requestMatchers("/cart/**").hasRole("USER")  // hasRole은 내부적으로 ROLE_ 붙임
//                        // 그 외는 인증 필요
//                        .anyRequest().authenticated()
//                )
//                .httpBasic(b -> b.disable())
//                .formLogin(f -> f.disable());
//        return http.build();
//    }
//    @Bean
//    PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
//}