package com.example.finalproject.domain.stores.dto.projection;

/**
 * [Projection] 인기 메뉴 Top-N 결과
 * ------------------------------------------------------
 * - Repository 네이티브 쿼리 결과를 Projection 으로 매핑
 * - menuId  : 메뉴 ID
 * - name    : 메뉴명
 * - quantity: 판매 수량 합계
 * - revenue : 매출액 합계 (quantity × 메뉴 단가)
 * ! 주의:
 * - revenue 계산 시 현재 메뉴 단가(m.price)를 사용
 * - 과거 주문 당시 단가를 보존하려면 order_items.unit_price 사용 권장
 */
public interface TopMenuRow {
    Long getMenuId();
    String getName();
    Long getQuantity();
    Long getRevenue();
}
