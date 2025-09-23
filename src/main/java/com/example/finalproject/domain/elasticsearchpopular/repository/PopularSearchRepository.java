package com.example.finalproject.domain.elasticsearchpopular.repository;

import com.example.finalproject.domain.elasticsearchpopular.entity.PopularSearch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PopularSearchRepository extends JpaRepository<PopularSearch, Long> {
    List<PopularSearch> findTop10ByOrderByRankAsc();
    List<PopularSearch> findTop10ByRegionOrderByRankAsc(String region);
}
