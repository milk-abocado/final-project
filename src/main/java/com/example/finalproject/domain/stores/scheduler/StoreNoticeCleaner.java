package com.example.finalproject.domain.stores.scheduler;

import com.example.finalproject.domain.stores.repository.StoreNoticeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class StoreNoticeCleaner {

    private final StoreNoticeRepository repo;

    // 만료 공지 전역 삭제
    @Transactional
    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul") // 매 분 0초에 실행
    public void purgeExpiredGlobally() {
        // 현재 시각(now) 기준으로 ends_at < now 인 공지를 일괄 삭제
        repo.deleteExpired(LocalDateTime.now());
    }
}
