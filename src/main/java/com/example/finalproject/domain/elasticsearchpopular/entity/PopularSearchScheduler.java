package com.example.finalproject.scheduler;

import com.example.finalproject.domain.elasticsearchpopular.service.PopularSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PopularSearchScheduler {

    private final PopularSearchService popularSearchService;

    // 1시간마다 실행 (예: 매 시각 0분)
    @Scheduled(cron = "0 0 * * * *")
    public void updatePopularSearches() throws Exception {
        popularSearchService.aggregateAndStore();
    }
}
