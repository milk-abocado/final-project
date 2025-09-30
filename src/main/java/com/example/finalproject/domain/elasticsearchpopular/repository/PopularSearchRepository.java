package com.example.finalproject.domain.elasticsearchpopular.repository;

import com.example.finalproject.domain.elasticsearchpopular.entity.PopularSearch;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PopularSearchRepository extends JpaRepository<PopularSearch, Long> {

     // 가변 N개 조회
     @Query("SELECT p FROM PopularSearch p WHERE p.region = :region ORDER BY p.count DESC")
     List<PopularSearch> findTopNByRegionOrderByCountDesc(String region, Pageable pageable);

     List<PopularSearch> findTop10ByRegionOrderByCountDesc(String region);
}
