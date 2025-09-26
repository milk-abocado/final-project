package com.example.finalproject.domain.stores.auth;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

/**
 * SpringDataWebConfig
 * -------------------------------------------------
 * - Spring Data의 페이지 직렬화 방식을 전역 설정
 * - PageImpl을 그대로 직렬화할 경우 경고가 발생하므로,
 *   안정적인 JSON 구조(DTO 형태)로 변환하도록 지정
 * - @EnableSpringDataWebSupport 옵션 중 pageSerializationMode = VIA_DTO 를 사용
 *   -> Page<T> 반환 시 Jackson이 고정된 구조의 JSON으로 직렬화
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class SpringDataWebConfig {
}
