package com.example.finalproject.domain.stores.recommendation;

/**
 * RecommendationSortBy
 * -------------------------------------------------
 * 가게 추천 API의 정렬 기준을 나타내는 enum
 * ///
 * - TRENDING : 최근 주문 수, 리뷰 수, 평균 별점을 복합적으로 반영한 점수
 * - RATING   : 베이지안 평균 별점 기준 (리뷰 수가 적어도 왜곡 최소화)
 * - REVIEWS  : 최근 N일간 리뷰 수 기준
 * - DISTANCE : 사용자 좌표(lat, lng) 기준으로 가까운 순
 */
public enum RecommendationSortBy {
    TRENDING,    // 최근 주문 수/리뷰 수/평점 복합 점수 기준
    RATING,      // 베이지안 평균 별점 기준
    REVIEWS,     // 최근 N일간 리뷰 수 기준
    DISTANCE;    // 사용자 좌표 기준 거리순

    /**
     * 문자열을 enum 으로 안전하게 변환
     * - null 또는 잘못된 값이면 기본값 TRENDING 반환
     *
     * @param by 쿼리 파라미터 값
     * @return RecommendationSortBy
     */
    public static RecommendationSortBy from(String by) {
        if (by == null) return TRENDING;
        try {
            return RecommendationSortBy.valueOf(by.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return TRENDING;
        }
    }
}
