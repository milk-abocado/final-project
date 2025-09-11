package com.example.finalproject.domain.stores.scheduler;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// @Scheduled 메서드들을 활성화하는 전역 설정
@Configuration
@EnableScheduling   // 스케줄러 기능 켜기
public class SchedulingConfig {}
