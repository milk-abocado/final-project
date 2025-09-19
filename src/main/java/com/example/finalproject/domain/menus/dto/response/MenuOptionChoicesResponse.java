package com.example.finalproject.domain.menus.dto.response;


import com.example.finalproject.domain.menus.entity.MenuOptionChoices;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MenuOptionChoicesResponse {

    private Long id;
    private String choiceName;
    private Integer extraPrice;

    public MenuOptionChoicesResponse(MenuOptionChoices choice) {
        this.id = choice.getId();
        this.choiceName = choice.getChoiceName();
        this.extraPrice = choice.getExtraPrice();
    }
}
