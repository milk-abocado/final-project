package com.example.finalproject.domain.menus.dto.response;

import com.example.finalproject.domain.menus.entity.MenuOptions;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MenuOptionsResponse {
    private Long id;
    private String optionsName;
    private Boolean isRequired;
    private Integer minSelect;
    private Integer maxSelect;
    private List<MenuOptionChoicesResponse> choices;

    public MenuOptionsResponse(MenuOptions option, List<MenuOptionChoicesResponse> choices) {
        this.id = option.getId();
        this.optionsName = option.getOptionsName();
        this.isRequired = option.getIsRequired();
        this.minSelect = option.getMinSelect();
        this.maxSelect = option.getMaxSelect();
        this.choices = choices; // 그대로 넣으면 됨
    }
}
