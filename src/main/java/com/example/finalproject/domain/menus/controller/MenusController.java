package com.example.finalproject.domain.menus.controller;


import com.example.finalproject.domain.menus.dto.request.MenusRequest;
import com.example.finalproject.domain.menus.dto.response.MenusResponse;
import com.example.finalproject.domain.menus.service.MenusService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/owners/stores/{storeId}/menus")
public class MenusController {

    private final MenusService menusService;

    @PostMapping
    public MenusResponse createMenu(@PathVariable Long storeId,
                                    @RequestBody MenusRequest request) {
        return menusService.createMenu(storeId, request);
    }

    @PatchMapping("/{menuId}")
    public MenusResponse updateMenu(@PathVariable Long storeId,
                                    @PathVariable Long menuId,
                                    @RequestBody MenusRequest request) {
        return menusService.updateMenu(menuId, storeId, request);
    }

    @DeleteMapping("/{menuId}")
    public String deleteMenu(@PathVariable Long storeId,
                             @PathVariable Long menuId) {
        menusService.deleteMenu(menuId, storeId);
        return "메뉴가 삭제되었습니다.";
    }
}
