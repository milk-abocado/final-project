package com.example.finalproject.domain.stores.menu;

import com.example.finalproject.domain.stores.dto.response.MenuSummaryResponse;

import java.util.List;

/**
 * MenuReader
 * -------------------------------------------------
 * - 특정 가게(storeId)의 메뉴 목록을 읽어오는 역할을 담당하는 인터페이스
 * - 헥사고날 아키텍처(Port & Adapter)에서 "포트(Port)" 역할
 * - 실제 구현체(@Service)는 DB 조회, 외부 API 연동 등 다양한 방식으로 작성 가능
 */
@FunctionalInterface
public interface MenuReader {
    /**
     * 특정 가게의 메뉴 목록 조회
     *
     * @param storeId 가게 ID
     * @return 메뉴 요약 리스트 (없으면 빈 리스트)
     */
    List<MenuSummaryResponse> findMenusOfStore(Long storeId);
}
