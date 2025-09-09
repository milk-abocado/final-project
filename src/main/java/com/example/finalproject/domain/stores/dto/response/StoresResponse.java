package com.example.finalproject.domain.stores.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoresResponse {

    private Long id;                   // 가게 ID (PK)
    private Long ownerId;              // 가게 소유주(사장님) ID (FK)
    private String name;               // 가게 이름
    private String address;            // 가게 주소
    private Integer minOrderPrice;     // 최소 주문 금액
    private LocalTime opensAt;         // 영업 시작 시간
    private LocalTime closesAt;        // 영업 종료 시간

    private Integer deliveryFee;       // 배달비

    private boolean isOpenNow;         // 현재 영업 중인지 여부 (계산 필드, DB 컬럼 아님)
                                       // Service 계층에서 now 기준 opensAt~closesAt 범위 확인 후 세팅

    private LocalDateTime createdAt;   // 생성 시각 (DB에서 자동 기록)
    private LocalDateTime updatedAt;   // 수정 시각 (DB에서 자동 갱신)
}
