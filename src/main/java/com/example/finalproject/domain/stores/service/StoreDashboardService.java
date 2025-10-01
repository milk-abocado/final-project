package com.example.finalproject.domain.stores.service;

import com.example.finalproject.domain.stores.dto.projection.OrderBucketRow;
import com.example.finalproject.domain.stores.dto.projection.ReviewRow;
import com.example.finalproject.domain.stores.dto.projection.SummaryRow;
import com.example.finalproject.domain.stores.dto.projection.TopMenuRow;
import com.example.finalproject.domain.stores.dto.response.StoreDashboardResponse;
import com.example.finalproject.domain.stores.exception.StoresApiException;
import com.example.finalproject.domain.stores.exception.StoresErrorCode;
import com.example.finalproject.domain.stores.repository.StoreDashboardRepository;
import com.example.finalproject.domain.stores.repository.StoresRepository;
import com.example.finalproject.domain.users.entity.Users;
import com.example.finalproject.domain.users.repository.UsersRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(Transactional.TxType.SUPPORTS)
public class StoreDashboardService {

    private final StoresRepository storesRepository;
    private final UsersRepository usersRepository;
    private final StoreDashboardRepository dashboardRepository;

    /**
     * [가게 대시보드 조회]
     * ---------------------------------------------------
     * 처리 단계:
     * (0) 파라미터 검증
     *      - grain: "day" | "month"
     *      - from <= to
     *      - 기본값: (to=오늘, from=to-30일)
     * (1) 현재 로그인 사용자 조회
     *      - SecurityContextHolder → email → Users 엔티티
     * (2) 가게 소유권 검증
     *      - owner_id == 로그인 사용자 id
     * (3) 기간 경계 변환
     *      - 입력: KST(LocalDate)
     *      - 변환: 그대로 yyyy-MM-dd 문자열 사용
     *      - DB created_at 컬럼이 이미 KST 시각으로 저장되므로
     *        UTC 변환 불필요
     * (4) 시계열 데이터 (orders/revenue by bucket)
     *      - grain=day   → YYYY-MM-DD
     *      - grain=month → YYYY-MM
     * (5) 요약 통계 (orders, revenue, AOV)
     * (6) 인기 메뉴 Top-N
     *      - 매출 계산: 메뉴 현재 단가(m.price) 기준
     * (7) 리뷰 통계 (avgRating, count)
     * (8) 고객 통계 (unique, repeat, repeatRate%)
     * (9) 응답 DTO 구성
     *      - range / series / summary / topMenus / reviews / customers
     */
    public StoreDashboardResponse getDashboard(Long storeId, LocalDate from, LocalDate to, String grain, int topN) {
        // (0) 파라미터 검증
        if (!"day".equals(grain) && !"month".equals(grain)) {
            throw new StoresApiException(StoresErrorCode.BAD_REQUEST, "grain은 day 또는 month만 허용됩니다.");
        }

        // 기본값: to가 null 이면 오늘(KST), from이 null 이면 to-30일
        ZoneId KST = ZoneId.of("Asia/Seoul");
        LocalDate todayKst = ZonedDateTime.now(KST).toLocalDate();
        to   = (to   == null) ? todayKst : to;
        from = (from == null) ? to.minusDays(30) : from;

        if (from.isAfter(to)) {
            throw new StoresApiException(StoresErrorCode.BAD_REQUEST, "from은 to보다 이후일 수 없습니다.");
        }

        // (1) 현재 사용자 조회 (SecurityContextHolder → email → Users)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = (auth != null) ? auth.getName() : null;
        Users me = usersRepository.findByEmailIgnoreCaseAndDeletedFalse(email)
                .orElseThrow(() -> new StoresApiException(StoresErrorCode.UNAUTHORIZED, "로그인 정보를 확인해 주세요."));

        // (2) 소유권 검증
        if (!storesRepository.existsByIdAndOwner_Id(storeId, me.getId())) {
            throw new StoresApiException(StoresErrorCode.FORBIDDEN, "본인 소유 가게만 조회할 수 있습니다.");
        }

        // (3) 기간 경계 생성
        String fromDate = from.toString();
        String toDate   = to.toString();

        // (4) 시계열 데이터 조회 (일/월 bucket 기준)
        List<OrderBucketRow> rows = dashboardRepository.aggregateOrdersAndRevenue(storeId, fromDate, toDate, grain);
        List<StoreDashboardResponse.Point> series = rows.stream()
                .map(r -> new StoreDashboardResponse.Point(
                        r.getBucket(),
                        safeLong(r.getOrders()),
                        safeLong(r.getRevenue())
                ))
                .toList();

        // (5) 요약 통계 조회 (orders, revenue, AOV)
        List<SummaryRow> sumList = dashboardRepository.summary(storeId, fromDate, toDate);
        SummaryRow sum = sumList.isEmpty() ? null : sumList.get(0);
        long totalOrders = (sum == null) ? 0L : safeLong(sum.getOrders());
        long revenue     = (sum == null) ? 0L : safeLong(sum.getRevenue());
        long aov         = (sum == null) ? 0L : safeLong(sum.getAov());

        // (6) 인기 메뉴 Top-N 조회
        List<TopMenuRow> top = dashboardRepository.topMenus(storeId, fromDate, toDate, topN);
        List<Map<String, Object>> topMenus = top.stream().map(r -> Map.<String, Object>of(
                "menuId",   safeLong(r.getMenuId()),
                "name",     r.getName(),
                "quantity", safeLong(r.getQuantity()),
                "revenue",  safeLong(r.getRevenue())
        )).toList();

        // (7) 리뷰 통계 조회 (평균 평점, 리뷰 개수)
        List<ReviewRow> rvList = dashboardRepository.reviewStats(storeId, fromDate, toDate);
        ReviewRow rv = rvList.isEmpty() ? null : rvList.get(0);
        double avgRating = (rv == null || rv.getAvgRating() == null) ? 0.0 : rv.getAvgRating();
        long reviewCount = (rv == null) ? 0L : safeLong(rv.getCount());

        // (8) 고객 통계 조회
        long uniq = safeLong(dashboardRepository.uniqueCustomers(storeId, fromDate, toDate));
        long rep  = safeLong(dashboardRepository.repeatCustomers(storeId, fromDate, toDate));
        double repeatRate = (uniq == 0) ? 0.0 : round2((double) rep * 100.0 / (double) uniq);

        // (9) 응답 DTO 구성
        StoreDashboardResponse res = new StoreDashboardResponse();
        res.setRange(Map.of(
                "from", from.toString(),
                "to", to.toString(),
                "grain", grain,
                "timezone", "Asia/Seoul"
        ));
        res.setSeries(series);
        res.setSummary(Map.of(
                "orders", totalOrders,
                "revenue", revenue,
                "aov", aov
        ));
        res.setTopMenus(topMenus);
        res.setReviews(Map.of(
                "avgRating", avgRating,
                "count", reviewCount
        ));
        res.setCustomers(Map.of(
                "unique", uniq,
                "repeat", rep,
                "repeatRate", repeatRate
        ));
        return res;
    }

    // ---- 유틸리티 ----
    /** null → 0 변환 & Number → long 캐스팅 */
    private static long safeLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number n) return n.longValue();
        return Long.parseLong(o.toString());
    }

    /** 소수점 둘째 자리 반올림 */
    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}