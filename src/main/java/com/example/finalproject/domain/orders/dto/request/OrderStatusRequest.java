package com.example.finalproject.domain.orders.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderStatusRequest {
    private String status; // 'WAITING', 'ACCEPTED', 'DELIVERING', 'COMPLETED', 'REJECTED', 'CANCELED'
}
