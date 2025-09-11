package com.example.finalproject.domain.orders.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrdersRequest {
    // 쿠폰 & 포인트 추후 구현 예정
     private Long usedCouponId;
     private Integer usedPoints;
}
