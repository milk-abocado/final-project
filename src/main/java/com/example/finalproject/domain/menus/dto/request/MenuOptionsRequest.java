package com.example.finalproject.domain.menus.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MenuOptionsRequest {
    private String optionsName;
    private Boolean isRequired;
    private Integer minSelect;
    private Integer maxSelect;
    private List<MenuOptionChoicesRequest> choices;
}
