package com.example.finalproject.domain.stores.repository;

import com.example.finalproject.domain.stores.entity.UserStar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * UserStarRepository
 * -------------------------------------------------
 * - 즐겨찾기(UserStar) 엔티티용 JPA Repository
 * - 사용자와 가게 간의 즐겨찾기 관계를 조회/검증/삭제하는 쿼리 메서드 정의
 */
public interface UserStarRepository extends JpaRepository<UserStar, Long> {

    // 특정 사용자가 특정 가게를 즐겨찾기 했는지 여부 확인
    boolean existsByUser_IdAndStore_Id(Long userId, Long storeId);

    // 특정 사용자가 등록한 즐겨찾기 개수
    long countByUser_Id(Long userId);

    // 특정 사용자와 가게 간의 즐겨찾기 단건 조회
    Optional<UserStar> findByUser_IdAndStore_Id(Long userId, Long storeId);

    // 폐업된 가게는 즐겨찾기 목록에서 제외
    // 특정 사용자의 즐겨찾기 중 등록 순(createdAt ASC)으로 최대 10개 조회
    List<UserStar> findTop10ByUser_IdAndStore_ActiveTrueOrderByCreatedAtAsc(Long userId);

    // 특정 사용자의 전체 즐겨찾기 목록 (등록 순 ASC)
    List<UserStar> findAllByUser_IdAndStore_ActiveTrueOrderByCreatedAtAsc(Long userId);
}
