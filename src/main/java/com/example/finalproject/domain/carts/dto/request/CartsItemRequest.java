package com.example.finalproject.domain.carts.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CartsItemRequest {
    private Long menuId;
    private int amount;
    private List<CartsOptionRequest> options;
}
