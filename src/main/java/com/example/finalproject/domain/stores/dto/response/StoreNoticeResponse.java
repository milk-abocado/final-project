package com.example.finalproject.domain.stores.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class StoreNoticeResponse {

    private Long id;                       // 공지 ID (PK)
    private Long storeId;                  // 가게 ID (FK)
    private String content;                // 공지 내용
    private LocalDateTime startsAt;        // 공지 시작 시각
    private LocalDateTime endsAt;          // 공지 종료 시각
    private Integer minDurationHours;      // 최소 유지 시간 (시간 단위)
    private Integer maxDurationDays;       // 최대 유지 기간 (일 단위)

    private boolean active;                // 현재 활성 상태 여부 (계산 필드)
                                           // Service에서 now 기준으로 계산 후 세팅

    private LocalDateTime createdAt;       // 생성 시각
    private LocalDateTime updatedAt;       // 수정 시각
}
