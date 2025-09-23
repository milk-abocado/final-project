package com.example.finalproject.domain.stores.dto.response;

import lombok.Builder;

import java.time.LocalTime;

/**
 * StoreRecommendItemResponse
 * -------------------------------------------------
 * 가게 추천 API 응답 전용 DTO
 * - 추천 기준(by=trending, rating, reviews, distance)에 따라 일부 필드가 null일 수 있음
 */
@Builder
public record StoreRecommendItemResponse(
        Long id,            // 가게 ID
        String name,        // 가게 이름
        String address,     // 가게 주소
        String category,    // 가게 카테고리 (store_categories 기준)
                            // 가게가 여러 카테고리에 속한 경우, 쿼리에서 MIN(category) 또는 GROUP_CONCAT 처리 결과
        Double avgRating,   // 평균 별점 (reviews 기반), 리뷰가 없으면 0.0 또는 null
        Long reviewCount,   // 리뷰 수, 특정 기간(reviews/trending) 또는 전체(rating/distance) 기준
        Double distanceKm,  // 거리 (킬로미터), by=distance 요청 시만 값이 채워짐
        Double score,       // 추천 점수
                            // by=trending: 주문/리뷰/평점을 가중 합산한 score
                            // by=rating: 베이지안 평균 점수
        Boolean openNow,    // 현재 영업 중 여부, opensAt/closesAt 기준으로 계산됨
        LocalTime opensAt,  // 오픈 시각 (HH:mm:ss)
        LocalTime closesAt  // 마감 시각 (HH:mm:ss)
) {}
