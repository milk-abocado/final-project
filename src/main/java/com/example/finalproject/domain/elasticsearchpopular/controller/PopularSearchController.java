package com.example.finalproject.domain.elasticsearchpopular.controller;

import com.example.finalproject.domain.elasticsearchpopular.entity.PopularSearches;
import com.example.finalproject.domain.elasticsearchpopular.service.PopularSearchService;
import com.example.finalproject.domain.elasticsearchpopular.service.PopularSearchSyncService;
import com.example.finalproject.domain.searches.service.SearchesService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/popular-searches")
@RequiredArgsConstructor
public class PopularSearchController {

    private final PopularSearchService popularSearchService;
    private final SearchesService searchesService;
    private final PopularSearchSyncService popularSearchSyncService;


    //DB Top N 조회
    @GetMapping("/popular/db")
    public List<PopularSearches> getTopFromDB(
            @RequestParam String region,
            @RequestParam(defaultValue = "10") int topN) {
        return popularSearchService.getTopFromDB(region, topN);
    }

    //수동 동기화
    @PostMapping("/sync")
    public Map<String, String> manualSync() throws Exception {
        popularSearchSyncService.syncPopularSearches();
        return Map.of("status", "synced");
    }

    @PostMapping("/searches")
    public Map<String, String> search(@RequestBody Map<String, String> body) throws BadRequestException {
        String keyword = body.get("keyword");
        String region = body.get("region");
        Long userId = Long.parseLong(body.get("userId"));

        // DB + Redis 기록
        searchesService.recordSearch(region, keyword, userId);

        return Map.of("status", "ok");
    }

    @GetMapping
    public List<PopularSearches> getTopByRegion(
            @RequestParam String region,
            @RequestParam(defaultValue = "10") int topN) {
        return popularSearchService.getTopFromDB(region, topN);
    }

    @GetMapping("/autocomplete")
    public List<String> autoComplete(
            @RequestParam String keyword,
            @RequestParam String region
    ) throws Exception {
        int maxResults = 10; //필요에 따라 변경
        return popularSearchService.autoComplete(keyword, region, maxResults);
    }

    public PopularSearchSyncService getPopularSearchSyncService() {
        return popularSearchSyncService;
    }
}

