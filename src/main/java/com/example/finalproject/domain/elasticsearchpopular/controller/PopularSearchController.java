package com.example.finalproject.domain.elasticsearchpopular.controller;

import com.example.finalproject.domain.elasticsearchpopular.service.PopularSearchService;
import io.jsonwebtoken.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/searches")
public class PopularSearchController {

    private final PopularSearchService searchService;

    @GetMapping("/suggest")
    public List<String> suggest(@RequestParam String region, @RequestParam String q) throws IOException, java.io.IOException {
        return searchService.suggestKeywords(region, q);
    }
}

