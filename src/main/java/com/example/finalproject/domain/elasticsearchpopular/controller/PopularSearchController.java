package com.example.finalproject.domain.elasticsearchpopular.controller;

import com.example.finalproject.domain.elasticsearchpopular.entity.PopularSearch;
import com.example.finalproject.domain.elasticsearchpopular.repository.PopularSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/popular-searches")
@RequiredArgsConstructor
public class PopularSearchController {

    private final PopularSearchRepository popularSearchRepository;

    //region 파라미터 없으면 전국 인기검색어 반환
    @GetMapping
    public List<PopularSearch> getPopularSearches(@RequestParam(required = false) Integer page, @RequestParam(required = false) String region) {
        if (region != null) {
            return popularSearchRepository.findTop10ByRegionOrderByRankAsc(region);
        }
        return popularSearchRepository.findTop10ByOrderByRankAsc();
    }
}
