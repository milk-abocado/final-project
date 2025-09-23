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
     * - 시간 비교: UTC 경계 (fromUtc <= created_at < toUtc)
     * - 버킷 표시: SELECT 단계에서만 KST(+09:00) 변환 → 라벨링 전용
     *   - grain='day'   → YYYY-MM-DD
     *   - grain='month' → YYYY-MM
     */
    @Query(value = """
        SELECT
          CASE WHEN :grain = 'month'
               THEN DATE_FORMAT(CONVERT_TZ(o.created_at, '+00:00', '+09:00'), '%Y-%m')
               ELSE DATE(CONVERT_TZ(o.created_at, '+00:00', '+09:00'))
          END AS bucket,
          COUNT(*) AS orders,
          COALESCE(SUM(o.total_price), 0) AS revenue
        FROM orders o
        WHERE o.store_id = :storeId
          AND o.status = 'COMPLETED'
          AND o.created_at >= :fromUtc
          AND o.created_at <  :toUtc
        GROUP BY bucket
        ORDER BY bucket
    """, nativeQuery = true)
    List<OrderBucketRow> aggregateOrdersAndRevenue(
            @Param("storeId") Long storeId,
            @Param("fromUtc") String fromUtc,
            @Param("toUtc")   String toUtc,
            @Param("grain")   String grain
    );

    /**
     * [요약] 주문 수 / 총 매출 / AOV
     * - 시간 비교: UTC 경계
     * - AOV(평균 객단가): revenue / orders
     *   - orders=0인 경우 NULL → ROUND(NULL)로 안전 처리됨
     */
    @Query(value = """
        SELECT
          COUNT(*)                                                   AS orders,
          COALESCE(SUM(o.total_price),0)                             AS revenue,
          ROUND(COALESCE(SUM(o.total_price),0) / NULLIF(COUNT(*),0)) AS aov
        FROM orders o
        WHERE o.store_id = :storeId
          AND o.status = 'COMPLETED'
          AND o.created_at >= :fromUtc
          AND o.created_at <  :toUtc
    """, nativeQuery = true)
    List<SummaryRow> summary(@Param("storeId") Long storeId,
                             @Param("fromUtc") String fromUtc,
                             @Param("toUtc")   String toUtc);

    /**
     * [인기 메뉴 Top-N]
     * - 기준: 판매 수량 DESC → 동률 시 매출 DESC
     * - 시간 비교: UTC 경계
     * - 주의: 매출 계산은 `m.price` 현재값을 사용
     *         → 과거 주문 시점의 단가를 반영하려면 `order_items.unit_price` 사용 권장
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
          AND o.status = 'COMPLETED'
          AND o.created_at >= :fromUtc
          AND o.created_at <  :toUtc
        GROUP BY oi.menu_id
        ORDER BY quantity DESC, revenue DESC
        LIMIT :limit
    """, nativeQuery = true)
    List<TopMenuRow> topMenus(@Param("storeId") Long storeId,
                              @Param("fromUtc") String fromUtc,
                              @Param("toUtc")   String toUtc,
                              @Param("limit")   int limit);

    /**
     * [리뷰 통계] 평균 평점 & 리뷰 개수
     * - 조건: 삭제되지 않은 리뷰(is_deleted=FALSE)만 포함
     * - 시간 비교: UTC 경계
     * - 평점 평균: 소수점 둘째 자리까지 반올림
     */
    @Query(value = """
        SELECT
          ROUND(AVG(r.rating), 2) AS avgRating,
          COUNT(*)                AS count
        FROM reviews r
        WHERE r.store_id = :storeId
          AND r.is_deleted = FALSE
          AND r.created_at >= :fromUtc
          AND r.created_at <  :toUtc
    """, nativeQuery = true)
    List<ReviewRow> reviewStats(@Param("storeId") Long storeId,
                                @Param("fromUtc") String fromUtc,
                                @Param("toUtc")   String toUtc);

    /**
     * [고유 고객 수]
     * - 특정 기간 내 COMPLETED 주문을 가진 서로 다른 사용자 수
     * - 시간 비교: UTC 경계
     */
    @Query(value = """
        SELECT COUNT(DISTINCT o.user_id) AS uniq
        FROM orders o
        WHERE o.store_id = :storeId
          AND o.status = 'COMPLETED'
          AND o.created_at >= :fromUtc
          AND o.created_at <  :toUtc
    """, nativeQuery = true)
    Long uniqueCustomers(@Param("storeId") Long storeId,
                         @Param("fromUtc") String fromUtc,
                         @Param("toUtc")   String toUtc);

    /**
     * [재방문 고객 수]
     * - 특정 기간 내 COMPLETED 주문을 2회 이상 가진 사용자 수
     * - 시간 비교: UTC 경계
     */
    @Query(value = """
        SELECT COUNT(*) FROM (
          SELECT o.user_id
          FROM orders o
          WHERE o.store_id = :storeId
            AND o.status = 'COMPLETED'
            AND o.created_at >= :fromUtc
            AND o.created_at <  :toUtc
          GROUP BY o.user_id
          HAVING COUNT(*) >= 2
        ) t
    """, nativeQuery = true)
    Long repeatCustomers(@Param("storeId") Long storeId,
                         @Param("fromUtc") String fromUtc,
                         @Param("toUtc")   String toUtc);
}
