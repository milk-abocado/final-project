package com.example.finalproject.domain.menus.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MenusRequest {

    private String name;
    private Integer price;
    private String status; // "ACTIVE", "DELETED", "SOLD_OUT"
    private List<String> categories; // ["한식", "덮밥"] 등
    private List<MenuOptionsRequest> options;

}
