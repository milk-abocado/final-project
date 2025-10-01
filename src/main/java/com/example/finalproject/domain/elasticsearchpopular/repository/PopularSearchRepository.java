package com.example.finalproject.domain.elasticsearchpopular.repository;

import com.example.finalproject.domain.elasticsearchpopular.entity.PopularSearches;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * PopularSearchRepository
 * 인기 검색어 엔티티(PopularSearches)에 대한 데이터 접근 계층
 */
@Repository
public interface PopularSearchRepository extends JpaRepository<PopularSearches, Long> {

     /**
      * 지역별 상위 N개의 인기 검색어를 조회
      * - Pageable 파라미터로 N개 제한
      * - searchCount DESC, updatedAt DESC 정렬
      */
     @Query("""
           SELECT p FROM PopularSearches p
           WHERE p.region = :region
           ORDER BY p.searchCount DESC, p.updatedAt DESC
           """)
     List<PopularSearches> findTopByRegion(@Param("region") String region, Pageable pageable);

     /**
      * 특정 지역 + 키워드 단건 조회
      */
     Optional<PopularSearches> findByRegionAndKeyword(String region, String keyword);

     /**
      * [벌크] 해당 region의 모든 레코드 ranking=0
      * - Top 리스트가 비어 있을 때 사용
      */
     @Modifying(clearAutomatically = true, flushAutomatically = true)
     @Query("UPDATE PopularSearches p SET p.ranking = 0 WHERE p.region = :region")
     int resetAllRankingByRegion(@Param("region") String region);

     /**
      * [벌크] 해당 region에서 주어진 keywords를 제외한 나머지의 ranking=0
      * - Top 리스트가 비어있지 않을 때 사용
      * - 파라미터 keywords는 비어있지 않다고 가정 (비면 NOT IN () 에러 가능)
      */
     @Modifying(clearAutomatically = true, flushAutomatically = true)
     @Query("""
           UPDATE PopularSearches p
           SET p.ranking = 0
           WHERE p.region = :region
             AND p.keyword NOT IN :keywords
           """)
     int resetRankingByRegionExcluding(@Param("region") String region,
                                       @Param("keywords") Collection<String> keywords);
}
