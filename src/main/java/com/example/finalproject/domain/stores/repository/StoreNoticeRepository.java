package com.example.finalproject.domain.stores.repository;

import com.example.finalproject.domain.stores.entity.StoresNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface StoreNoticeRepository extends JpaRepository<StoresNotice, Long> {

    /** 현재 시각 기준 활성 공지 (PK = storeId) — 앱 시간이 기준 */
    @Query("""
        select n
        from StoresNotice n
        where n.id = :storeId
          and n.startsAt <= :now
          and n.endsAt   >= :now
    """)
    Optional<StoresNotice> findActiveOneByStoreId(@Param("storeId") Long storeId,
                                                  @Param("now") LocalDateTime now);

    /** 현재 시각 기준 활성 공지 존재 여부 (exists 쿼리) — 앱 시간이 기준 */
    @Query("""
        select (count(n) > 0)
        from StoresNotice n
        where n.id = :storeId
          and n.startsAt <= :now
          and n.endsAt   >= :now
    """)
    boolean existsActiveByStoreId(@Param("storeId") Long storeId,
                                  @Param("now") LocalDateTime now);

    /** 만료 공지 전역 삭제 (스케줄러/관리자 용) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from StoresNotice n where n.endsAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);

    /** 만료 공지 특정 가게만 삭제 (서비스 진입 시 선제 정리 용) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from StoresNotice n where n.id = :storeId and n.endsAt < :now")
    int deleteExpiredByStore(@Param("storeId") Long storeId, @Param("now") LocalDateTime now);
}
