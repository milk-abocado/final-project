package com.example.finalproject.domain.stores.service;

import com.example.finalproject.domain.stores.dto.response.StoreRecommendItemResponse;
import com.example.finalproject.domain.stores.recommendation.RecommendationSortBy;
import com.example.finalproject.domain.stores.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.Objects;

/**
 * StoreRecommendationService
 * -------------------------------------------------
 * - 추천 기준(by)에 따라 Repository 네이티브 쿼리를 호출하고,
 *   Object[] 결과를 StoreRecommendItemResponse DTO로 매핑
 * - Repository의 SELECT 컬럼 순서와 아래 mapRow* 메서드의 인덱스가 반드시 일치해야 함
 */
@Service
@RequiredArgsConstructor
public class StoreRecommendationService {

    private final RecommendationRepository recommendationRepository;

    // 베이지안 평균 보정 파라미터
    // 서비스 전체에 쌓인 모든 가게 평균 평점을 기반으로 설정해둔 값
    private static final double GLOBAL_MEAN_RATING = 4.2; // m
    // 리뷰가 몇 개 이상 있어야 평균이 신뢰할 만하다고 볼지 기준이 되는 값
    private static final double SMOOTHING_C = 20.0;       // C

    /**
     * 추천 조회 진입점
     *
     * @param category   카테고리(옵션, null 이면 전체, 공백만 들어오면 null로 정규화됨
     * @param by         정렬 기준(enum)
     * @param period     "7d", "30d" 형식 (reviews/trending 에서 사용)
     * @param minReviews 최소 리뷰 수 (rating 기준에서 사용, null 허용)
     * @param lat        위도 (distance 기준에서 필수)
     * @param lng        경도 (distance 기준에서 필수)
     * @param openNow    true면 현재 영업 중인 가게만
     * @param pageable   페이지네이션
     */
    @Transactional(readOnly = true)
    public Page<StoreRecommendItemResponse> recommend(
            String category,
            RecommendationSortBy by,
            String period,
            Integer minReviews,
            Double lat,
            Double lng,
            boolean openNow,
            Pageable pageable
    ) {
        int days = parseDays(period);   // "7d" -> 7, "30d" -> 30 (이상치 방어 포함)
        String normalized = normalizeCategory(category); // "" → null

        return switch (by) {
            case RATING -> recommendationRepository
                    .findByBayesianRating(normalized, minReviews, openNow, GLOBAL_MEAN_RATING, SMOOTHING_C, pageable)
                    .map(this::mapRowForRating);

            case REVIEWS -> recommendationRepository
                    .findByRecentReviews(normalized, days, openNow, pageable)
                    .map(this::mapRowForReviews);

            case DISTANCE -> {
                if (lat == null || lng == null) {
                    // 좌표가 없으면 안전하게 rating 로직으로 대체
                    yield recommendationRepository
                            .findByBayesianRating(normalized, minReviews, openNow, GLOBAL_MEAN_RATING, SMOOTHING_C, pageable)
                            .map(this::mapRowForRating);
                }
                yield recommendationRepository
                        .findByDistance(normalized, lat, lng, openNow, pageable)
                        .map(this::mapRowForDistance);
            }

            case TRENDING -> recommendationRepository
                    .findTrending(normalized, days, openNow, pageable)
                    .map(this::mapRowForTrending);
        };
    }

    // "7d" → 7, 잘못된 입력은 30으로 폴백
    private int parseDays(String period) {
        if (period == null) return 30;
        String p = period.trim().toLowerCase();
        if (p.endsWith("d")) {
            try {
                int v = Integer.parseInt(p.substring(0, p.length() - 1));
                return Math.max(1, Math.min(v, 365)); // 과도한 값 방어
            } catch (NumberFormatException ignored) {}
        }
        return 30;
    }

    // 공백만 들어온 카테고리를 null로 정규화
    private String normalizeCategory(String category) {
        if (category == null) return null;
        String t = category.trim();
        return t.isEmpty() ? null : t;
    }

    // ====== Row 매핑 (native query 결과 Object[] 가정) ======

    // RATING 정렬 결과 매핑
    private StoreRecommendItemResponse mapRowForRating(Object[] row) {
        // SELECT: id, name, address, MIN(sc.category) AS category,
        //         avg_rating, review_cnt, bayes_score, opens_at, closes_at
        Long id = toLong(row[0]);
        String name = (String) row[1];
        String address = (String) row[2];
        String category = safeStr(row[3]);
        Double avgRating = toDouble(row[4]);
        Long reviewCnt = toLong(row[5]);
        Double bayes = toDouble(row[6]);
        LocalTime opensAt = toLocalTime(row[7]);
        LocalTime closesAt = toLocalTime(row[8]);

        return StoreRecommendItemResponse.builder()
                .id(id).name(name).address(address).category(category)
                .avgRating(avgRating).reviewCount(reviewCnt)
                .bayesScore(bayes)
                .openNow(isOpenNow(opensAt, closesAt))
                .opensAt(opensAt).closesAt(closesAt)
                .build();
    }

    // REVIEWS 정렬 결과 매핑
    private StoreRecommendItemResponse mapRowForReviews(Object[] row) {
        // SELECT: id, name, address, MIN(sc.category) AS category,
        //         recent_reviews, avg_rating, opens_at, closes_at
        Long id = toLong(row[0]);
        String name = (String) row[1];
        String address = (String) row[2];
        String category = safeStr(row[3]);
        Long recentReviews = toLong(row[4]);
        Double avgRating = toDouble(row[5]);
        LocalTime opensAt = toLocalTime(row[6]);
        LocalTime closesAt = toLocalTime(row[7]);

        return StoreRecommendItemResponse.builder()
                .id(id).name(name).address(address).category(category)
                .avgRating(avgRating).reviewCount(recentReviews)
                .openNow(isOpenNow(opensAt, closesAt))
                .opensAt(opensAt).closesAt(closesAt)
                .build();
    }

    // DISTANCE 정렬 결과 매핑
    private StoreRecommendItemResponse mapRowForDistance(Object[] row) {
        // SELECT: id, name, address, MIN(sc.category) AS category,
        //         distance_km, avg_rating, review_cnt, opens_at, closes_at
        Long id = toLong(row[0]);
        String name = (String) row[1];
        String address = (String) row[2];
        String category = safeStr(row[3]);
        Double distanceKm = toDouble(row[4]);
        Double avgRating = toDouble(row[5]);
        Long reviewCnt = toLong(row[6]);
        LocalTime opensAt = toLocalTime(row[7]);
        LocalTime closesAt = toLocalTime(row[8]);

        return StoreRecommendItemResponse.builder()
                .id(id).name(name).address(address).category(category)
                .distanceKm(distanceKm).avgRating(avgRating).reviewCount(reviewCnt)
                .openNow(isOpenNow(opensAt, closesAt))
                .opensAt(opensAt).closesAt(closesAt)
                .build();
    }

    // TRENDING 정렬 결과 매핑
    private StoreRecommendItemResponse mapRowForTrending(Object[] row) {
        // SELECT: id, name, address, MIN(sc.category) AS category,
        //         orders_30d, reviews_30d, avg_rating, score, opens_at, closes_at
        Long id = toLong(row[0]);
        String name = (String) row[1];
        String address = (String) row[2];
        String category = safeStr(row[3]);
        // Long orders30d = toLong(row[4]); // 필요 시 사용
        Long reviews30d = toLong(row[5]);
        Double avgRating = toDouble(row[6]);
        Double trendScore = toDouble(row[7]);
        LocalTime opensAt = toLocalTime(row[8]);
        LocalTime closesAt = toLocalTime(row[9]);

        return StoreRecommendItemResponse.builder()
                .id(id).name(name).address(address).category(category)
                .reviewCount(reviews30d).avgRating(avgRating)
                .trendScore(trendScore)
                .openNow(isOpenNow(opensAt, closesAt))
                .opensAt(opensAt).closesAt(closesAt)
                .build();
    }

    // ====== 공통 유틸 ======
    /** 자정 넘김(예: 18:00~02:00) 케이스 포함한 현재 영업 중 판단 */
    private boolean isOpenNow(LocalTime open, LocalTime close) {
        if (open == null || close == null) return false;
        var now = LocalTime.now();
        if (open.isBefore(close)) {
            return !now.isBefore(open) && !now.isAfter(close);
        } else {
            return !now.isBefore(open) || !now.isAfter(close);
        }
    }

    /** TIME / TIMESTAMP / String → LocalTime 변환 (네이티브 쿼리 대응) */
    private LocalTime toLocalTime(Object o) {
        if (o == null) return null;
        if (o instanceof LocalTime lt) return lt;
        if (o instanceof Time t) return t.toLocalTime();
        if (o instanceof Timestamp ts) return ts.toLocalDateTime().toLocalTime();
        if (o instanceof String s) {
            try { return LocalTime.parse(s); } catch (Exception ignored) {}
        }
        return null;
    }

    /** 공통 변환 유틸 */
    private Long toLong(Object o)   { return (o == null) ? null : ((Number) o).longValue(); }
    private Double toDouble(Object o){ return (o == null) ? null : ((Number) o).doubleValue(); }
    private String safeStr(Object o) { return (o == null) ? null : Objects.toString(o); }
}