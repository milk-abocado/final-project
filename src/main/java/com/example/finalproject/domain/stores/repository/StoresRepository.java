package com.example.finalproject.domain.stores.repository;

import com.example.finalproject.domain.stores.entity.Stores;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 가게 Repository
 * - JpaRepository<Stores, Long> 상속하여 기본 CRUD 제공
 * - 가게 주소 중복 여부, 오너별 가게 개수 확인 메서드 포함
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
}
