package com.example.finalproject.domain.stores.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class StoreDashboardResponse {
    private Map<String, Object> range;              // 조회 구간: {from, to, grain}
    private List<Point> series;                     // 일/월 버킷별 주문·매출 시계열
    private Map<String, Object> summary;            // 주문·매출 요약 (총합, AOV 등)
    private List<Map<String, Object>> topMenus;     // 인기 메뉴 Top-N
    private Map<String, Object> reviews;            // 리뷰 통계 (평점 평균, 개수)
    private Map<String, Object> customers;          // 고객 통계 (고유 고객 수, 재구매 고객 수/율)

    /**
     * 시계열 데이터 포인트
     * - bucket: "YYYY-MM-DD" 또는 "YYYY-MM"
     * - orders: 주문 수
     * - revenue: 매출액
     */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class Point {
        private String bucket;  // 버킷 단위 (기간 라벨)
        private long orders;    // 해당 버킷(일/월) 동안 발생한 주문 수
        private long revenue;   // 해당 버킷(일/월) 동안 발생한 총 매출액
    }
}
