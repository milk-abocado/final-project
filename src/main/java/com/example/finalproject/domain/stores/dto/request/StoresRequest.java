package com.example.finalproject.domain.stores.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoresRequest {

    // 가게 이름 (필수, 공백 불가)
    @NotBlank
    private String name;

    // 가게 주소 (필수, 공백 불가)
    @NotBlank
    private String address;

    // 최소 주문 금액 (최소 1 이상)
    @Min(1)
    private Integer minOrderPrice;

    // 가게 오픈 시간 (필수)
    @NotNull
    private LocalTime opensAt;

    // 가게 마감 시간 (필수)
    @NotNull
    private LocalTime closesAt;

    // 배달비 (0원 이상)
    @PositiveOrZero
    private Integer deliveryFee;
}
