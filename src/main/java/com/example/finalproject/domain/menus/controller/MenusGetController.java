package com.example.finalproject.domain.menus.controller;


import com.example.finalproject.domain.menus.dto.response.MenusResponse;
import com.example.finalproject.domain.menus.service.MenusService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stores/{storeId}/menus")
public class MenusGetController {

    private final MenusService menusService;

    @GetMapping("/{menuId}")
    public MenusResponse getMenu(@PathVariable Long storeId,
                                 @PathVariable Long menuId) {
        return menusService.getMenu(menuId);
    }

    @GetMapping
    public List<MenusResponse> getMenusByStore(@PathVariable Long storeId) {
        return menusService.getMenusByStore(storeId);
    }
}
