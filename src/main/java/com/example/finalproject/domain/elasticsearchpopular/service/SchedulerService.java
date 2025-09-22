package com.example.finalproject.domain.elasticsearchpopular.service;

import io.jsonwebtoken.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SchedulerService {
    private final PopularSearchService popularSearchService;

    @Scheduled(cron = " 0 0 * * * *") //매 정시마다 실행
    public void updatePopularSearches() throws IOException {
        popularSearchService.aggregationPopularSearches();
    }
}
