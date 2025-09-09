package com.example.finalproject.domain.stores.repository;

import com.example.finalproject.domain.stores.category.StoreCategory;
import com.example.finalproject.domain.stores.entity.Stores;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

/**
 * StoresRepository
 * -------------------------------------------------
 * - 가게 엔티티(Stores)에 대한 기본 CRUD 및 커스텀 조회 제공
 * - JPA 메서드 쿼리와 네이티브 쿼리를 혼합 사용
 */
public interface StoresRepository extends JpaRepository<Stores, Long> {

    /**
     * 주소(address)로 가게 존재 여부 확인
     * - 중복된 주소 등록 방지용
     */
    boolean existsByAddress(String address);

    /** 동일 주소를 가진 다른 가게 존재 여부 (자기 자신 제외) */
    boolean existsByAddressAndIdNot(String address, Long id);

    /**
     * 특정 사장님(ownerId)이 소유한 가게 개수 조회
     * - 가게 수 제한(LIMIT_EXCEEDED) 같은 비즈니스 로직에서 활용 가능
     */
    long countByOwner_Id(Long ownerId);

    /** 활성(ACTIVE=true) 가게만 카운트 → 신규 생성 제한에 사용 */
    long countByOwner_IdAndActiveTrue(Long ownerId);

    /** 조회 시 ACTIVE만 보이게 하고 싶을 때 사용 */
    Optional<Stores> findByIdAndActiveTrue(Long id);

    /**
     * 이름 부분검색 + 좌표 반경 필터 + 거리 정렬
     * -------------------------------------------------
     * - 입력 좌표(:lat, :lng)를 기준으로 ST_Distance_Sphere 로 거리(m) 계산
     * - :keyword 가 비어있지 않으면 이름 LIKE 검색
     * - :radiusMeters 가 null 이 아니면 해당 반경(m) 이내만 필터
     * - 정렬은 항상 거리 오름차순
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
            @Param("category") String category,  // 가게 카테고리
            Pageable pageable                    // 페이지/정렬 정보
    );

    /** 주어진 카테고리 중 하나라도 매칭 */
    @Query("""
        select distinct s
        from Stores s
        join s.categoryLinks l
        where s.active = true
          and l.category in :cats
    """)
    Page<Stores> findActiveByAnyCategoryIn(@Param("cats") Collection<StoreCategory> cats, Pageable pageable);

}
