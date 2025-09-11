package com.example.finalproject.domain.carts.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CartsItemResponse {
    private String cartItemId = UUID.randomUUID().toString();
    private Long menuId;
    private String menuName;
    private Integer amount;
    private Integer price;
    private Integer totalPrice;
    private List<CartsOptionResponse> options;
    private LocalDateTime updatedAt;
}
