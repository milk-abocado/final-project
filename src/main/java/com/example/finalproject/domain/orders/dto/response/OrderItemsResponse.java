package com.example.finalproject.domain.orders.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderItemsResponse {
    private Long orderItemId;
    private Long menuId;
    private String menuName;
    private Integer amount;
    private List<OrderOptionsResponse> options;
    private Integer price;
    private Integer totalPrice;
}
