package com.example.finalproject.domain.stores.repository;

import com.example.finalproject.domain.stores.category.StoreCategory;
import com.example.finalproject.domain.stores.entity.StoreCategoryLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * StoreCategoryLinkRepository
 * -------------------------------------------------
 * - 가게와 카테고리(StoreCategory) 매핑 엔티티(StoreCategoryLink) 전용 Repository
 * - JpaRepository 상속으로 기본 CRUD 제공
 * - 가게별 카테고리 관리에 필요한 커스텀 메서드 정의
 */
public interface StoreCategoryLinkRepository extends JpaRepository<StoreCategoryLink, Long> {

    // 특정 가게(storeId)에 등록된 모든 카테고리 링크 조회
    List<StoreCategoryLink> findByStore_Id(Long storeId);

    // 특정 가게에 해당 카테고리가 존재하는지 여부 확인
    boolean existsByStore_IdAndCategory(Long storeId, StoreCategory category);

    // 특정 가게에 등록된 카테고리 개수 조회
    long countByStore_Id(Long storeId);

    // 특정 가게에서 지정된 카테고리만 삭제
    void deleteByStore_IdAndCategory(Long storeId, StoreCategory category);

    // 특정 가게의 모든 카테고리 삭제
    void deleteByStore_Id(Long storeId);
}
