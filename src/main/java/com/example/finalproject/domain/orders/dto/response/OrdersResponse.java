package com.example.finalproject.domain.orders.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class OrdersResponse {
    private Long orderId;
    private Long userId;
    private Long storeId;
    private String storeName;
    private String address;
    private String status;
    private List<OrderItemsResponse> items;
    private Integer orderTotalPrice;
    // 쿠폰 & 포인트 추후 구현 예정
    private Integer usedPoints;
    private OrderCouponsResponse appliedCoupon;
    private Integer totalPrice;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
