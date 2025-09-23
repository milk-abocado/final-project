package com.example.finalproject.domain.elasticsearchpopular.repository;

import com.example.finalproject.domain.elasticsearchpopular.entity.PopularSearch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PopularSearchRepository extends JpaRepository<PopularSearch, Long> {
}
