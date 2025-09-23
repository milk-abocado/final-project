package com.example.finalproject.domain.stores.repository;

import com.example.finalproject.domain.stores.entity.Stores;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * RecommendationRepository
 * -------------------------------------------------
 * 가게 추천/랭킹 관련 쿼리를 제공하는 Repository
 * - Stores 엔티티를 기반으로 함
 * - 네이티브 쿼리(@Query(nativeQuery = true))로 작성
 */
public interface RecommendationRepository extends JpaRepository<Stores, Long> {

    /*
      공통: openNow 필터 (자정 넘김까지 처리)
        opens_at < closes_at  -> TIME(NOW()) BETWEEN opens_at AND closes_at
        opens_at > closes_at  -> TIME(NOW()) >= opens_at OR TIME(NOW()) <= closes_at
      아래 네 쿼리 모두 동일한 조건을 씀.
     */

    /**
     * [별점 기준 추천]
     * - 베이지안 평균 점수로 정렬
     * - COUNT(r.id) >= minReviews 조건 충족 가게만 포함
     *
     * @param category   필터링할 카테고리 (NULL → 전체)
     * @param minReviews 최소 리뷰 수 (NULL → 제한 없음)
     * @param openNow    true → 현재 영업 중인 가게만
     * @param globalMean 전체 평균 별점 (m, 베이지안 보정용)
     * @param smoothingC 스무딩 상수 (C, 베이지안 보정용)
     * @param pageable   페이지네이션
     * @return Page<Object[]> : [id, name, address, category, avg_rating, review_cnt, bayes_score, opens_at, closes_at]
     */
    @Query(value = """
    SELECT s.id, s.name, s.address,
           sc.category,
           COALESCE(SUM(r.rating) / NULLIF(COUNT(DISTINCT r.id),0), 0) AS avg_rating,
           COUNT(DISTINCT r.id) AS review_cnt,
           ((COALESCE(SUM(r.rating) / NULLIF(COUNT(DISTINCT r.id),0), 0) * COUNT(DISTINCT r.id) + :C * :m) / (COUNT(DISTINCT r.id) + :C) ) AS bayes_score,
           s.opens_at, s.closes_at
    FROM stores s
    LEFT JOIN (
        SELECT store_id, MIN(category) AS category
        FROM store_categories
        GROUP BY store_id
    ) sc ON sc.store_id = s.id
    LEFT JOIN reviews r ON r.store_id = s.id AND r.is_deleted = FALSE
    WHERE s.active = TRUE
      AND s.retired_at IS NULL
      AND (:category IS NULL OR sc.category = :category)
      AND (
            :openNow = FALSE
            OR (
                (s.opens_at < s.closes_at AND TIME(NOW()) BETWEEN s.opens_at AND s.closes_at)
             OR (s.opens_at > s.closes_at AND (TIME(NOW()) >= s.opens_at OR TIME(NOW()) <= s.closes_at))
            )
          )
    GROUP BY s.id
    HAVING (:minReviews IS NULL OR COUNT(r.id) >= :minReviews)
    ORDER BY bayes_score DESC, review_cnt DESC
    """,
            countQuery = """
    SELECT COUNT(*) FROM (
      SELECT s.id
      FROM stores s
      LEFT JOIN store_categories sc ON sc.store_id = s.id
      LEFT JOIN reviews r ON r.store_id = s.id AND r.is_deleted = FALSE
      WHERE s.active = TRUE
        AND s.retired_at IS NULL
        AND (:category IS NULL OR sc.category = :category)
        AND (
              :openNow = FALSE
              OR (
                  (s.opens_at < s.closes_at AND TIME(NOW()) BETWEEN s.opens_at AND s.closes_at)
               OR (s.opens_at > s.closes_at AND (TIME(NOW()) >= s.opens_at OR TIME(NOW()) <= s.closes_at))
              )
            )
      GROUP BY s.id
      HAVING (:minReviews IS NULL OR COUNT(r.id) >= :minReviews)
    ) t
    """,
            nativeQuery = true)
    Page<Object[]> findByBayesianRating(@Param("category") String category,
                                        @Param("minReviews") Integer minReviews,
                                        @Param("openNow") boolean openNow,
                                        @Param("m") double globalMean,
                                        @Param("C") double smoothingC,
                                        Pageable pageable);


    /**
     * [리뷰 수 기준 추천]
     * - 최근 N일 간의 리뷰 수 내림차순 정렬
     * - tie-breaker: 평균 별점(avg_rating)
     *
     * @param category 카테고리 필터
     * @param days     최근 N일 기준 (예: 7 → 최근 7일)
     * @param openNow  현재 영업 여부 필터
     * @param pageable 페이지네이션
     * @return Page<Object[]> : [id, name, address, category, recent_reviews, avg_rating, opens_at, closes_at]
     */
    @Query(value = """
    SELECT s.id, s.name, s.address,
           sc.category,
           COUNT(DISTINCT r.id) AS recent_reviews,
           COALESCE(SUM(r.rating) / NULLIF(COUNT(DISTINCT r.id),0), 0) AS avg_rating,
           s.opens_at, s.closes_at
    FROM stores s
    LEFT JOIN (
        SELECT store_id, MIN(category) AS category
        FROM store_categories
        GROUP BY store_id
    ) sc ON sc.store_id = s.id
    LEFT JOIN reviews r
      ON r.store_id = s.id
     AND r.is_deleted = FALSE
     AND r.created_at >= DATE_SUB(NOW(), INTERVAL :days DAY)
    WHERE s.active = TRUE
      AND s.retired_at IS NULL
      AND (:category IS NULL OR sc.category = :category)
      AND (
            :openNow = FALSE
            OR (
                (s.opens_at < s.closes_at AND TIME(NOW()) BETWEEN s.opens_at AND s.closes_at)
             OR (s.opens_at > s.closes_at AND (TIME(NOW()) >= s.opens_at OR TIME(NOW()) <= s.closes_at))
            )
          )
    GROUP BY s.id
    ORDER BY recent_reviews DESC, avg_rating DESC
    """,
            countQuery = """
    SELECT COUNT(*)
    FROM stores s
    LEFT JOIN store_categories sc ON sc.store_id = s.id
    WHERE s.active = TRUE
      AND s.retired_at IS NULL
      AND (:category IS NULL OR sc.category = :category)
      AND (
            :openNow = FALSE
            OR (
                (s.opens_at < s.closes_at AND TIME(NOW()) BETWEEN s.opens_at AND s.closes_at)
             OR (s.opens_at > s.closes_at AND (TIME(NOW()) >= s.opens_at OR TIME(NOW()) <= s.closes_at))
            )
          )
    """,
            nativeQuery = true)
    Page<Object[]> findByRecentReviews(@Param("category") String category,
                                       @Param("days") int days,
                                       @Param("openNow") boolean openNow,
                                       Pageable pageable);


    /**
     * [거리 기준 추천]
     * - 사용자 좌표(lat, lng)와 Haversine 공식을 이용해 거리 계산
     * - 거리 오름차순 → 별점 내림차순 → 리뷰 수 내림차순 정렬
     *
     * @param category 카테고리 필터
     * @param lat      사용자 위도
     * @param lng      사용자 경도
     * @param openNow  현재 영업 여부 필터
     * @param pageable 페이지네이션
     * @return Page<Object[]> : [id, name, address, category, distance_km, avg_rating, review_cnt, opens_at, closes_at]
     */
    @Query(value = """
    SELECT s.id, s.name, s.address,
           MIN(sc.category) AS category,
           (6371 * ACOS(
              COS(RADIANS(:lat)) * COS(RADIANS(s.latitude)) *
              COS(RADIANS(s.longitude) - RADIANS(:lng)) +
              SIN(RADIANS(:lat)) * SIN(RADIANS(s.latitude))
           )) AS distance_km,
           COALESCE(avg_r.avg_rating,0) AS avg_rating,
           COALESCE(avg_r.review_cnt,0) AS review_cnt,
           s.opens_at, s.closes_at
    FROM stores s
    LEFT JOIN store_categories sc ON sc.store_id = s.id
    LEFT JOIN (
      SELECT store_id, AVG(rating) AS avg_rating, COUNT(*) AS review_cnt
      FROM reviews
      WHERE is_deleted = FALSE
      GROUP BY store_id
    ) avg_r ON avg_r.store_id = s.id
    WHERE s.active = TRUE
      AND s.retired_at IS NULL
      AND (:category IS NULL OR sc.category = :category)
      AND (
            :openNow = FALSE
            OR (
                (s.opens_at < s.closes_at AND TIME(NOW()) BETWEEN s.opens_at AND s.closes_at)
             OR (s.opens_at > s.closes_at AND (TIME(NOW()) >= s.opens_at OR TIME(NOW()) <= s.closes_at))
            )
          )
    GROUP BY s.id
    ORDER BY distance_km, avg_rating DESC, review_cnt DESC
    """,
            countQuery = """
    SELECT COUNT(*)
    FROM stores s
    LEFT JOIN store_categories sc ON sc.store_id = s.id
    WHERE s.active = TRUE
      AND s.retired_at IS NULL
      AND (:category IS NULL OR sc.category = :category)
      AND (
            :openNow = FALSE
            OR (
                (s.opens_at < s.closes_at AND TIME(NOW()) BETWEEN s.opens_at AND s.closes_at)
             OR (s.opens_at > s.closes_at AND (TIME(NOW()) >= s.opens_at OR TIME(NOW()) <= s.closes_at))
            )
          )
    """,
            nativeQuery = true)
    Page<Object[]> findByDistance(@Param("category") String category,
                                  @Param("lat") double lat,
                                  @Param("lng") double lng,
                                  @Param("openNow") boolean openNow,
                                  Pageable pageable);


    /**
     * [트렌딩 기준 추천]
     * - 최근 N일 간의 주문 수, 리뷰 수, 평균 별점을 가중합(score)으로 계산
     *   score = 0.6 * log(1+orders) + 0.3 * avg_rating + 0.1 * log(1+reviews)
     * - 점수 내림차순 정렬
     *
     * @param category 카테고리 필터
     * @param days     최근 N일 기준 (예: 30 → 최근 30일)
     * @param openNow  현재 영업 여부 필터
     * @param pageable 페이지네이션
     * @return Page<Object[]> : [id, name, address, category, orders_30d, reviews_30d, avg_rating, score, opens_at, closes_at]
     */
    @Query(value = """
    WITH r AS (
      SELECT store_id,
             COUNT(*) AS reviews_30d,
             COALESCE(AVG(rating),0) AS avg_rating
      FROM reviews\s
      WHERE is_deleted = FALSE
        AND created_at >= DATE_SUB(NOW(), INTERVAL :days DAY)
      GROUP BY store_id
    ), o AS (
      SELECT store_id,
             COUNT(*) AS orders_30d
      FROM orders
      WHERE created_at >= DATE_SUB(NOW(), INTERVAL :days DAY)
      GROUP BY store_id
    )
    SELECT s.id, s.name, s.address,
           MIN(sc.category) AS category,
           COALESCE(o.orders_30d, 0) as orders_30d,
           COALESCE(r.reviews_30d, 0) as reviews_30d,
           COALESCE(r.avg_rating, 0) as avg_rating,
           (0.6 * LOG(1 + COALESCE(o.orders_30d, 0))
          + 0.3 * COALESCE(r.avg_rating, 0)
          + 0.1 * LOG(1 + COALESCE(r.reviews_30d, 0))) AS score,
           s.opens_at, s.closes_at
    FROM stores s
    LEFT JOIN store_categories sc ON sc.store_id = s.id
    LEFT JOIN r ON r.store_id = s.id
    LEFT JOIN o ON o.store_id = s.id
    WHERE s.active = TRUE
      AND s.retired_at IS NULL
      AND (:category IS NULL OR sc.category = :category)
      AND (
            :openNow = FALSE
            OR (
                (s.opens_at < s.closes_at AND TIME(NOW()) BETWEEN s.opens_at AND s.closes_at)
             OR (s.opens_at > s.closes_at AND (TIME(NOW()) >= s.opens_at OR TIME(NOW()) <= s.closes_at))
            )
          )
    GROUP BY s.id
    ORDER BY score DESC
   \s""",
            countQuery = """
    SELECT COUNT(*)
    FROM stores s
    LEFT JOIN store_categories sc ON sc.store_id = s.id
    WHERE s.active = TRUE
      AND s.retired_at IS NULL
      AND (:category IS NULL OR sc.category = :category)
      AND (
            :openNow = FALSE
            OR (
                (s.opens_at < s.closes_at AND TIME(NOW()) BETWEEN s.opens_at AND s.closes_at)
             OR (s.opens_at > s.closes_at AND (TIME(NOW()) >= s.opens_at OR TIME(NOW()) <= s.closes_at))
            )
          )
    """,
            nativeQuery = true)
    Page<Object[]> findTrending(@Param("category") String category,
                                @Param("days") int days,
                                @Param("openNow") boolean openNow,
                                Pageable pageable);
}
