package com.example.finalproject.domain.menus.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class MenuOptionsRequest {
    private String optionsName;
    private Boolean isRequired;
    private Integer minSelect;
    private Integer maxSelect;
    private List<MenuOptionChoicesRequest> choices;
}
