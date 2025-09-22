package com.example.finalproject.domain.stores.dto.projection;

/**
 * [Projection] 주문 집계 결과 (일/월 버킷 단위)
 * ------------------------------------------------------
 * - Repository 네이티브 쿼리 결과를 인터페이스 기반 Projection 으로 매핑
 * - bucket : 집계 단위 (YYYY-MM-DD 또는 YYYY-MM)
 * - orders : 주문 건수
 * - revenue: 총 매출액
 */
public interface OrderBucketRow {
    String getBucket();
    Long getOrders();
    Long getRevenue();
}
