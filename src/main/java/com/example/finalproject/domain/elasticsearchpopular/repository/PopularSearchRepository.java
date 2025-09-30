package com.example.finalproject.domain.elasticsearchpopular.repository;

import com.example.finalproject.domain.elasticsearchpopular.entity.PopularSearches;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PopularSearchRepository extends JpaRepository<PopularSearches, Long> {

     // 가변 N개 조회
     @Query
     List<PopularSearches> findTopNByRegionOrderByCountDesc(String region, Pageable pageable);

     List<PopularSearches> findTop10ByRegionOrderByCountDesc(String region);
}
