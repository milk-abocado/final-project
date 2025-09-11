package com.example.finalproject.domain.stores.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 외부 API 호출용 WebClient 설정 클래스
 * -------------------------------------------------
 * - Spring WebFlux 의 WebClient 를 Bean 으로 등록하여
 *   애플리케이션 전역에서 주입받아 사용 가능하게 함
 * - 여기서는 Kakao Local API 호출 전용 WebClient 를 생성
 *   (baseUrl 을 "https://dapi.kakao.com" 으로 지정)
 */
@Configuration
public class WebClients {
    @Bean
    public WebClient kakaoWebClient() {
        // Kakao Local API 전용 WebClient 생성 및 Bean 등록
        return WebClient.builder()
                .baseUrl("https://dapi.kakao.com") // 기본 요청 URL
                .build();
    }
}
