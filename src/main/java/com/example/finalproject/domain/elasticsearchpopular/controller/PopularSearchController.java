package com.example.finalproject.domain.elasticsearchpopular.controller;

import com.example.finalproject.domain.elasticsearchpopular.service.PopularSearchService;
import com.example.finalproject.domain.searches.service.SearchesService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/popular-searches")
@RequiredArgsConstructor
public class PopularSearchController {

    private final PopularSearchService popularSearchService;
    private final SearchesService searchesService;

    @PostMapping("/searches")
    public Map<String, String> search(@RequestBody Map<String, String> body) {
        String keyword = body.get("keyword");
        String region = body.get("region");
        Long userId = Long.parseLong(body.get("userId"));

        // DB + Redis 기록
        searchesService.recordSearch(region, keyword);

        return Map.of("status", "ok");
    }

    @GetMapping("/{region}")
    public List<Map<String, Object>> getTop10ByRegion(@PathVariable String region) throws Exception {
        return popularSearchService.getTop10ByRegion(region);
    }

    @GetMapping("/autocomplete")
    public List<String> autoComplete(
            @RequestParam String keyword,
            @RequestParam String region
    ) throws Exception {  // ← 여기 추가
        return popularSearchService.autoComplete(keyword, region);
    }
}

