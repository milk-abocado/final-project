package com.example.finalproject.domain.elasticsearchpopular.controller;

import com.example.finalproject.domain.elasticsearchpopular.entity.PopularSearch;
import com.example.finalproject.domain.elasticsearchpopular.repository.PopularSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/popular-searches")
public class PopularSearchController {

    private final PopularSearchRepository repository;

    // 지역별 인기 검색어 조회
    @GetMapping("/{region}")
    public List<PopularSearch> getPopularSearchesByRegion(@PathVariable String region) {
        return repository.findAll().stream()
                .filter(p -> p.getRegion().equals(region))
                .toList();
    }
}

