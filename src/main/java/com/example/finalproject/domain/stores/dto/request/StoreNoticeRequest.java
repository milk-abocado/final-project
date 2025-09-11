package com.example.finalproject.domain.stores.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class StoreNoticeRequest {

    // 공지 내용 (필수, 공백 불가)
    @NotBlank
    private String content;

    // 공지 시작 시각 (필수)
    @NotNull
    private LocalDateTime startsAt;

    // 공지 종료 시각 (필수)
    @NotNull
    private LocalDateTime endsAt;

    // 최소 유지 시간 (시간 단위, 0 이상)
    @NotNull @Min(0)
    private Integer minDurationHours;

    // 최대 유지 기간 (일 단위, 0 이상)
    @NotNull @Min(0)
    private Integer maxDurationDays;
}
