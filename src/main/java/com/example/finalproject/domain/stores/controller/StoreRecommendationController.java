package com.example.finalproject.domain.stores.controller;

import com.example.finalproject.domain.stores.dto.response.StoreRecommendItemResponse;
import com.example.finalproject.domain.stores.recommendation.RecommendationSortBy;
import com.example.finalproject.domain.stores.service.StoreRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * StoreRecommendationController
 * -------------------------------------------------
 * 가게 추천 API 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/stores")
public class StoreRecommendationController {

    private final StoreRecommendationService recommendationService;

    /**
     * 가게 추천 조회
     *
     * @param category   카테고리 필터 (예: "KOREAN", "CHICKEN", null → 전체)
     * @param by         정렬 기준 (trending, rating, reviews, distance)
     * @param period     기간 (기본: "30d")
     *                   - reviews/trending 에서 사용
     *                   - 예: "7d" → 최근 7일, "30d" → 최근 30일
     * @param minReviews 최소 리뷰 수 (rating 기준에서 사용)
     * @param lat        위도 (distance 기준에서 필수)
     * @param lng        경도 (distance 기준에서 필수)
     * @param openNow    true → 현재 영업 중인 가게만 추천
     * @param page       페이지 번호 (기본: 0)
     * @param size       페이지 크기 (기본: 20)
     * @return Page<StoreRecommendItemResponse>
     */
    @GetMapping("/recommendations")
    public ResponseEntity<Page<StoreRecommendItemResponse>> recommend(
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "trending") String by,
            @RequestParam(required = false, defaultValue = "30d") String period,
            @RequestParam(required = false) Integer minReviews,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false, defaultValue = "false") boolean openNow,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        RecommendationSortBy sortBy = RecommendationSortBy.from(by);
        PageRequest pr = PageRequest.of(page, size);
        return ResponseEntity.ok(
                recommendationService.recommend(category, sortBy, period, minReviews, lat, lng, openNow, pr)
        );
    }
}
