package com.example.finalproject.domain.elasticsearchpopular.controller;

import com.example.finalproject.domain.elasticsearchpopular.entity.PopularSearch;
import com.example.finalproject.domain.elasticsearchpopular.repository.PopularSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/popular-searches")
@RequiredArgsConstructor
public class PopularSearchController {

    private final PopularSearchRepository popularSearchRepository;

    @GetMapping
    public List<PopularSearch> getPopularSearches() {
        return popularSearchRepository.findTop10ByOrderByRankAsc();
    }
}
