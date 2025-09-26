package com.example.finalproject.domain.carts.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartsOptionResponse {
    private Long menuOptionChoicesId;
    private String name;
    private int extraPrice;
}
