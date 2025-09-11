package com.example.finalproject.domain.stores.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * MenuSummaryResponse
 * -------------------------------------------------
 * - 특정 가게의 메뉴를 요약 형태로 내려줄 때 사용하는 DTO
 * - 상세 메뉴 정보가 아닌 "간단한 목록" 용도로 활용
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MenuSummaryResponse {
    private Long id;          // 메뉴 ID
    private String name;      // 메뉴 이름
    private Integer price;    // 가격
    private Boolean available; // 주문 가능 여부
}
