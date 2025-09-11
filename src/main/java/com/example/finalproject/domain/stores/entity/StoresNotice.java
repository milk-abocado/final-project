package com.example.finalproject.domain.stores.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
@Table(name = "store_notices")  // DB 테이블 이름 지정
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class StoresNotice {

    // PK를 가게 PK와 공유
    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId // ← store_id를 PK로도 함께 사용
    @JoinColumn(name = "store_id", nullable = false, unique = true)
    private Stores store;

    // 공지 내용 (TEXT 타입)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 공지 시작 시간
    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    // 공지 종료 시간
    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    // 공지가 유지되어야 하는 최소 시간 (시간 단위)
    @Column(name = "min_duration_hours", nullable = false)
    private Integer minDurationHours;

    // 공지를 최대 며칠까지 유지할 수 있는지 (일 단위)
    @Column(name = "max_duration_days", nullable = false)
    private Integer maxDurationDays;

    // 생성 시각
    @org.springframework.data.annotation.CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 수정 시각
    @org.springframework.data.annotation.LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
