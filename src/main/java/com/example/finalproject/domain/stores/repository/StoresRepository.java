package com.example.finalproject.domain.stores.repository;


import com.example.finalproject.domain.stores.category.StoreCategory;
import com.example.finalproject.domain.stores.entity.Stores;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * StoresRepository
 * -------------------------------------------------
 * - 가게 엔티티(Stores)에 대한 기본 CRUD 및 커스텀 조회 제공
 * - JPA 메서드 쿼리와 네이티브 쿼리를 혼합 사용
 */
public interface StoresRepository extends JpaRepository<Stores, Long> {

    /**
     * 주소(address)로 가게 존재 여부 확인 (상태 무관)
     * - 필요에 따라 단순 조회용으로 사용 가능
     */
    boolean existsByAddress(String address);

    /**
     * 주소 중복 체크 (생성/수정용) — 운영 중인 가게와만 충돌 금지
     * - 폐업(retiredAt != null) 혹은 비활성(active=false)인 기록은 무시 → 주소 재사용 허용
     */
    boolean existsByAddressAndActiveTrueAndRetiredAtIsNull(String address);

    /**
     * 주소 중복 체크 (수정용: 자기 자신 제외) — 운영 중인 가게와만 충돌 금지
     */
    boolean existsByAddressAndActiveTrueAndRetiredAtIsNullAndIdNot(String address, Long id);

    /**
     * 특정 사장님(ownerId)이 소유한 가게 개수 조회
     * - 가게 수 제한(LIMIT_EXCEEDED) 같은 비즈니스 로직에서 활용 가능
     */
    long countByOwner_Id(Long ownerId);

    /** 활성(ACTIVE=true) 가게만 카운트 → 신규 생성 제한에 사용 */
    long countByOwner_IdAndActiveTrue(Long ownerId);

    /** 조회 시 ACTIVE만 보이게 하고 싶을 때 사용 (폐업 제외) */
    Optional<Stores> findByIdAndActiveTrueAndRetiredAtIsNull(Long id);

    /** 특정 오너가 소유한 가게의 storeId 리스트 가져오기 */
    @Query(value = "SELECT id FROM stores WHERE owner_id = :ownerId", nativeQuery = true)
    List<Long> findStoreIdsByOwnerId(@Param("ownerId") Long ownerId);

    /**
     * 이름 부분검색 + 좌표 반경 필터 + 거리 정렬
     * -------------------------------------------------
     * - 입력 좌표(:lat, :lng)를 기준으로 ST_Distance_Sphere로 거리(m) 계산
     * - :keyword가 비어 있지 않으면 이름 LIKE 검색
     * - :radiusMeters가 null 이 아니면 해당 반경(m) 이내만 필터
     * - 정렬은 항상 거리 오름차순
     * - 운영 중(active=true) + 미폐업(retired_at IS NULL)만 노출
     */
    @Query(value = """
        SELECT DISTINCT
          s.id, s.owner_id, s.name, s.address, s.latitude, s.longitude,
          s.min_order_price, s.opens_at, s.closes_at, s.delivery_fee,
          s.active, s.retired_at, s.created_at, s.updated_at,
          ST_Distance_Sphere(POINT(:lng, :lat), POINT(s.longitude, s.latitude)) AS distance
        FROM stores s
        LEFT JOIN store_categories sc ON sc.store_id = s.id
        WHERE s.active = true
          AND s.retired_at IS NULL
          AND (:keyword = '' OR s.name LIKE CONCAT('%', :keyword, '%'))
          AND (:category IS NULL OR sc.category = :category)
          AND (
            :radiusMeters IS NULL
            OR ST_Distance_Sphere(POINT(:lng, :lat), POINT(s.longitude, s.latitude)) <= :radiusMeters
          )
        ORDER BY distance
        """,
            countQuery = """
        SELECT COUNT(DISTINCT s.id)
        FROM stores s
        LEFT JOIN store_categories sc ON sc.store_id = s.id
        WHERE s.active = true
          AND s.retired_at IS NULL
          AND (:keyword = '' OR s.name LIKE CONCAT('%', :keyword, '%'))
          AND (:category IS NULL OR sc.category = :category)
          AND (
            :radiusMeters IS NULL
            OR ST_Distance_Sphere(POINT(:lng, :lat), POINT(s.longitude, s.latitude)) <= :radiusMeters
          )
        """,
            nativeQuery = true)
    Page<Object[]> searchWithDistanceRaw(
            @Param("keyword") String keyword,    // 부분 이름 검색어 (빈 문자열이면 전체)
            @Param("lat") double lat,            // 기준 위도
            @Param("lng") double lng,            // 기준 경도
            @Param("radiusMeters") Double radiusMeters, // 반경(m) (null 이면 무제한)
            @Param("category") String category,  // 가게 카테고리 (null 허용)
            Pageable pageable
    );

    /** 주어진 카테고리 중 하나라도 매칭 (운영 중 + 미폐업만) */
    @Query("""
        select distinct s
        from Stores s
        join s.categoryLinks l
        where s.active = true
          and s.retiredAt is null
          and l.category in :cats
    """)
    Page<Stores> findActiveByAnyCategoryIn(@Param("cats") Collection<StoreCategory> cats, Pageable pageable);
}
