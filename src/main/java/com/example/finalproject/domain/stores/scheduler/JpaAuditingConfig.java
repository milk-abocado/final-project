package com.example.finalproject.domain.stores.scheduler;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// JPA Auditing 활성화: @CreatedDate / @LastModifiedDate 자동 세팅
@Configuration
@EnableJpaAuditing  // 스프링 데이터 JPA 감시 기능 켜기
public class JpaAuditingConfig {}

