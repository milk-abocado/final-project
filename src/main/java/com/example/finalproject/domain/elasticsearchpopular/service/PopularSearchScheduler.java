package com.example.finalproject.domain.elasticsearchpopular.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PopularSearchScheduler {

    private final PopularSearchService popularSearchService;

    // 1시간마다 인기 검색어 집계
    @Scheduled(fixedRate = 3600000)
    public void refreshTopSearches() {
        // region별 Top10을 가져와 DB 또는 캐시 업데이트
        // popularSearchService.getTop10ByRegion("Seoul") 등
        System.out.println("Refreshing popular searches...");
    }
}