package com.example.finalproject.domain.menus.controller;


import com.example.finalproject.domain.menus.dto.response.MenusResponse;
import com.example.finalproject.domain.menus.dto.response.MenusSimpleResponse;
import com.example.finalproject.domain.menus.service.MenusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stores/{storeId}/menus")
public class MenusController {

    private final MenusService menusService;

    // 메뉴 단건 조회
    @GetMapping("/{menuId}")
    public ResponseEntity<?> getMenu(@PathVariable Long storeId,
                                 @PathVariable Long menuId) {
        MenusResponse menus = menusService.getMenu(menuId, storeId);
        return ResponseEntity.ok(menus);
    }

    // 가게별 메뉴 조회
    @GetMapping
    public ResponseEntity<?> getMenusByStore(@PathVariable Long storeId) {
        List<MenusSimpleResponse> menus = menusService.getMenusByStore(storeId);
        return ResponseEntity.ok(menus);
    }
}
