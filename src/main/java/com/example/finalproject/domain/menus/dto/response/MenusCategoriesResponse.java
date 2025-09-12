package com.example.finalproject.domain.menus.dto.response;

import com.example.finalproject.domain.menus.entity.MenuCategories;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MenusCategoriesResponse {
    private Long id;
    private String category;

    public MenusCategoriesResponse(MenuCategories category) {
        this.id = category.getId();
        this.category = category.getCategory();
    }
}
