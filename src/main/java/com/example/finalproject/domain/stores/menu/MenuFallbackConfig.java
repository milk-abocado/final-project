package com.example.finalproject.domain.stores.menu;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * MenuFallbackConfig
 * -------------------------------------------------
 * - MenuReader 인터페이스의 "기본 구현(fallback)"을 제공하는 설정 클래스
 * - 실제 @Service 로 MenuReader 구현체가 존재하지 않을 경우에만
 *   noop(동작하지 않는) 구현체를 Bean 으로 등록
 */
@Configuration
public class MenuFallbackConfig {

    /**
     * MenuReader 기본 구현 (noop)
     * - storeId 를 받아도 무시하고 항상 빈 리스트 반환
     * - 실제 메뉴 조회 기능이 필요하지 않은 상황에서 안전하게 동작
     */
    @Bean
    @ConditionalOnMissingBean(MenuReader.class)
    public MenuReader noopMenuReader() {
        return (Long storeId) -> List.of(); // 인터페이스와 정확히 동일한 시그니처/반환타입
    }
}
