package com.example.finalproject.domain.stores.dto.projection;

/**
 * [Projection] 요약 통계 결과
 * ------------------------------------------------------
 * - Repository 네이티브 쿼리 결과를 Projection 으로 매핑
 * - orders : 총 주문 건수
 * - revenue: 총 매출액
 * - aov    : 평균 객단가(Average Order Value = revenue / orders)
 */
public interface SummaryRow {
    Long getOrders();
    Long getRevenue();
    Long getAov();
}
