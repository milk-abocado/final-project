package com.example.finalproject.domain.menus.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MenusSimpleResponse {
    private Long id;
    private Long storeId;
    private String name;
    private Integer price;
    private String status;
    private List<MenusCategoriesResponse> categories;

    public MenusSimpleResponse(Long id, Long storeId, String name, Integer price, String status,
                               List<MenusCategoriesResponse> categories) {
        this.id = id;
        this.storeId = storeId;
        this.name = name;
        this.price = price;
        this.status = status;
        this.categories = categories;
    }
}
