package com.example.finalproject.domain.stores.repository;

import com.example.finalproject.domain.stores.dto.projection.OrderBucketRow;
import com.example.finalproject.domain.stores.dto.projection.ReviewRow;
import com.example.finalproject.domain.stores.dto.projection.SummaryRow;
import com.example.finalproject.domain.stores.dto.projection.TopMenuRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * StoreDashboardRepository
 * ---------------------------------------------------
 * - 가게 대시보드용 통계 집계 전용 Repository
 * - 모든 시간 비교는 UTC 기준 (fromUtc <= created_at < toUtc)
 * - SELECT 시점에서만 타임존 변환(KST +09:00) 적용
 */
@Repository
public interface StoreDashboardRepository extends JpaRepository<com.example.finalproject.domain.stores.entity.Stores, Long> {

    /**
     * [일/월 버킷 집계] 주문 수 & 매출 (완료 상태 기준)
     * - 기준: 주문 created_at (KST 로컬시각)
     * - 버킷 표시:
     *   - grain='day'   → DATE(created_at)
     *   - grain='month' → DATE_FORMAT(created_at, '%Y-%m')
     * - 기간 조건: DATE(created_at) BETWEEN :fromDate AND :toDate
     */
    @Query(value = """
        SELECT
          CASE WHEN :grain = 'month'
               THEN DATE_FORMAT(o.created_at, '%Y-%m')   -- 월 버킷
               ELSE DATE(o.created_at)                   -- 일 버킷
          END AS bucket,
          COUNT(*)                        AS orders,
          COALESCE(SUM(o.total_price),0)  AS revenue
        FROM orders o
        WHERE o.store_id = :storeId
          AND o.status   = 'COMPLETED'
          AND DATE(o.created_at) BETWEEN :fromDate AND :toDate
        GROUP BY bucket
        ORDER BY bucket
    """, nativeQuery = true)
    List<OrderBucketRow> aggregateOrdersAndRevenue(
            @Param("storeId") Long storeId,
            @Param("fromDate") String fromDate, // yyyy-MM-dd (KST 달력 날짜)
            @Param("toDate")   String toDate,   // yyyy-MM-dd (KST 달력 날짜)
            @Param("grain")    String grain
    );

    /**
     * [요약] 주문 수 / 총 매출 / AOV
     * - 기준: 주문 created_at (KST 로컬시각)
     * - 기간 조건: DATE(created_at) BETWEEN :fromDate AND :toDate
     * - AOV(평균 객단가): revenue / orders
     */
    @Query(value = """
        SELECT
          COUNT(*)                                                   AS orders,
          COALESCE(SUM(o.total_price),0)                             AS revenue,
          ROUND(COALESCE(SUM(o.total_price),0) / NULLIF(COUNT(*),0)) AS aov
        FROM orders o
        WHERE o.store_id = :storeId
          AND o.status   = 'COMPLETED'
          AND DATE(o.created_at) BETWEEN :fromDate AND :toDate
    """, nativeQuery = true)
    List<SummaryRow> summary(@Param("storeId") Long storeId,
                             @Param("fromDate") String fromDate,
                             @Param("toDate")   String toDate);


    /**
     * [인기 메뉴 Top-N]
     * - 기준: 판매 수량 DESC → 동률 시 매출 DESC
     * - 기간 조건: DATE(created_at) BETWEEN :fromDate AND :toDate
     * - 매출 계산은 메뉴 현재 단가(m.price) 사용
     *   (과거 주문 시점 단가 반영 필요시 order_items.unit_price 사용 권장)
     */
    @Query(value = """
        SELECT
          oi.menu_id                 AS menuId,
          MAX(m.name)                AS name,
          SUM(oi.quantity)           AS quantity,
          SUM(oi.quantity * m.price) AS revenue
        FROM orders o
        JOIN order_items oi ON oi.order_id = o.id
        JOIN menus m        ON m.id = oi.menu_id
        WHERE o.store_id = :storeId
          AND o.status   = 'COMPLETED'
          AND DATE(o.created_at) BETWEEN :fromDate AND :toDate
        GROUP BY oi.menu_id
        ORDER BY quantity DESC, revenue DESC
        LIMIT :limit
    """, nativeQuery = true)
    List<TopMenuRow> topMenus(@Param("storeId") Long storeId,
                              @Param("fromDate") String fromDate,
                              @Param("toDate")   String toDate,
                              @Param("limit")    int limit);

    /**
     * [리뷰 통계] 평균 평점 & 리뷰 개수
     * - 기준: 리뷰 created_at (KST 로컬시각)
     * - 조건: is_deleted = FALSE
     * - 기간 조건: DATE(created_at) BETWEEN :fromDate AND :toDate
     * - 평균 평점: 소수점 둘째 자리까지 반올림
     */
    @Query(value = """
        SELECT
          ROUND(AVG(r.rating), 2) AS avgRating,
          COUNT(*)                AS count
        FROM reviews r
        WHERE r.store_id   = :storeId
          AND r.is_deleted = FALSE
          AND DATE(r.created_at) BETWEEN :fromDate AND :toDate
    """, nativeQuery = true)
    List<ReviewRow> reviewStats(@Param("storeId") Long storeId,
                                @Param("fromDate") String fromDate,
                                @Param("toDate")   String toDate);

    /**
     * [고유 고객 수]
     * - 기준: 특정 기간 내 COMPLETED 주문이 있는 서로 다른 user_id 수
     * - 기간 조건: DATE(created_at) BETWEEN :fromDate AND :toDate
     */
    @Query(value = """
        SELECT COUNT(DISTINCT o.user_id) AS uniq
        FROM orders o
        WHERE o.store_id = :storeId
          AND o.status   = 'COMPLETED'
          AND DATE(o.created_at) BETWEEN :fromDate AND :toDate
    """, nativeQuery = true)
    Long uniqueCustomers(@Param("storeId") Long storeId,
                         @Param("fromDate") String fromDate,
                         @Param("toDate")   String toDate);


    /**
     * [재방문 고객 수]
     * - 기준: 특정 기간 내 COMPLETED 주문이 2회 이상인 사용자 수
     * - 기간 조건: DATE(created_at) BETWEEN :fromDate AND :toDate
     */
    @Query(value = """
        SELECT COUNT(*) FROM (
          SELECT o.user_id
          FROM orders o
          WHERE o.store_id = :storeId
            AND o.status   = 'COMPLETED'
            AND DATE(o.created_at) BETWEEN :fromDate AND :toDate
          GROUP BY o.user_id
          HAVING COUNT(*) >= 2
        ) t
    """, nativeQuery = true)
    Long repeatCustomers(@Param("storeId") Long storeId,
                         @Param("fromDate") String fromDate,
                         @Param("toDate")   String toDate);
}
