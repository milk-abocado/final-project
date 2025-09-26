package com.example.finalproject.domain.orders.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class OrderStatusResponse {
    private Long orderId;
    private Long storeId;
    private String status;
    private LocalDateTime updatedAt;
    private String message;
}
