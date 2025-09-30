package com.example.finalproject.domain.elasticsearchpopular.repository;

import com.example.finalproject.domain.elasticsearchpopular.entity.PopularSearches;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PopularSearchRepository extends JpaRepository<PopularSearches, Long> {

     // 지역별 상위 N개 검색어 조회 (동적 N개)
     @Query("SELECT p FROM PopularSearches p WHERE p.region = :region ORDER BY p.searchCount DESC")
     List<PopularSearches> findTopByRegion(@Param("region") String region, Pageable pageable);
}
