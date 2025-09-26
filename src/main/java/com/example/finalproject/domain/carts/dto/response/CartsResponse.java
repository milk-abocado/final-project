package com.example.finalproject.domain.carts.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CartsResponse {
    private Long userId;
    private Long storeId;
    private String storeName;
    private List<CartsItemResponse> items;
    private int cartTotalPrice;
    private LocalDateTime updatedAt;
}
