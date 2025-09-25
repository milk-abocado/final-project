package com.example.finalproject.domain.stores.dto.projection;

/**
 * [Projection] 리뷰 통계 결과
 * ------------------------------------------------------
 * - Repository 네이티브 쿼리 결과를 Projection 으로 매핑
 * - avgRating : 평균 평점 (소수점 둘째 자리까지 반올림)
 * - count     : 리뷰 개수 (is_deleted = FALSE 조건만 포함)
 */
public interface ReviewRow {
    Double getAvgRating();
    Long getCount();
}
