package com.example.finalproject.domain.menus.controller;


import com.example.finalproject.domain.menus.dto.request.MenuCategoryRequest;
import com.example.finalproject.domain.menus.dto.request.MenuOptionChoicesRequest;
import com.example.finalproject.domain.menus.dto.request.MenuOptionsRequest;
import com.example.finalproject.domain.menus.dto.request.MenusRequest;
import com.example.finalproject.domain.menus.dto.response.MenusResponse;
import com.example.finalproject.domain.menus.service.MenusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/owners/stores/{storeId}/menus")
public class MenusOwnerController {

    private final MenusService menusService;

    // 메뉴 생성
    @PostMapping
    public ResponseEntity<?> createMenu(
            Authentication authentication,
            @PathVariable Long storeId,
            @RequestBody MenusRequest request) {
        MenusResponse menus = menusService.createMenu(authentication, storeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(menus);
    }

    // 메뉴 수정
    @PatchMapping("/{menuId}")
    public ResponseEntity<?> updateMenu(
            Authentication authentication,
            @PathVariable Long storeId,
            @PathVariable Long menuId,
            @RequestBody MenusRequest request) {
        MenusResponse menus = menusService.updateMenu(authentication, menuId, storeId, request);
        return ResponseEntity.ok(menus);
    }

    // 메뉴 복구
    @PostMapping("/{menuId}/restore")
    public ResponseEntity<?> restoreMenu(
            Authentication authentication,
            @PathVariable Long storeId,
            @PathVariable Long menuId) {

        MenusResponse restoredMenu = menusService.restoreMenu(authentication, menuId, storeId);
        return ResponseEntity.ok(restoredMenu);
    }

    // 메뉴 삭제
    @DeleteMapping("/{menuId}")
    public ResponseEntity<?> deleteMenu(
            Authentication authentication,
            @PathVariable Long storeId,
            @PathVariable Long menuId) {
        menusService.deleteMenu(authentication, menuId, storeId);
        return ResponseEntity.ok("메뉴가 삭제되었습니다.");
    }

    // 카테고리 단건 추가
    @PostMapping("/{menuId}/categories")
    public ResponseEntity<?> addCategory(
            Authentication authentication,
            @PathVariable Long storeId,
            @PathVariable Long menuId,
            @RequestBody MenuCategoryRequest categoryName) {
        MenusResponse category = menusService.addCategory(menuId, categoryName, authentication, storeId);
        return ResponseEntity.status(HttpStatus.CREATED).body(category);
    }

    // 카테고리 단건 수정
    @PatchMapping("/{menuId}/categories/{categoryId}")
    public ResponseEntity<?> updateCategory(
            Authentication authentication,
            @PathVariable Long storeId,
            @PathVariable Long menuId,
            @PathVariable Long categoryId,
            @RequestBody MenuCategoryRequest newCategoryName) {
        MenusResponse category = menusService.updateCategory(menuId, categoryId, newCategoryName, authentication, storeId);
        return ResponseEntity.ok(category);
    }

    // 카테고리 단건 삭제
    @DeleteMapping("/{menuId}/categories/{categoryId}")
    public ResponseEntity<?> deleteCategory(
            Authentication authentication,
            @PathVariable Long storeId,
            @PathVariable Long menuId,
            @PathVariable Long categoryId) {
        menusService.deleteCategory(menuId, categoryId, authentication, storeId);
        return ResponseEntity.ok("카테고리가 삭제되었습니다.");
    }

    // 카테고리 전체 삭제
    @DeleteMapping("/{menuId}/categories")
    public ResponseEntity<?> deleteAllCategories(
            Authentication authentication,
            @PathVariable Long storeId,
            @PathVariable Long menuId) {
        menusService.deleteAllCategories(menuId, authentication, storeId);
        return ResponseEntity.ok("카테고리가 전체 삭제되었습니다.");
    }

    // 메뉴 옵션 단건 생성 (옵션 그룹 + 선택지)
    @PostMapping("/{menuId}/options")
    public ResponseEntity<?> createOptionGroup(
            Authentication authentication,
            @PathVariable Long storeId,
            @PathVariable Long menuId,
            @RequestBody MenuOptionsRequest request) {
        MenusResponse option = menusService.createOptionGroup(menuId, request, authentication, storeId);
        return ResponseEntity.status(HttpStatus.CREATED).body(option);
    }

    // 옵션 그룹 단건 삭제
    @DeleteMapping("/{menuId}/options/{optionGroupId}")
    public ResponseEntity<?> deleteOptionGroup(
            Authentication authentication,
            @PathVariable Long storeId,
            @PathVariable Long menuId,
            @PathVariable Long optionGroupId) {
        menusService.deleteOptionGroup(menuId, optionGroupId, authentication, storeId);
        return ResponseEntity.ok("옵션 그룹이 삭제되었습니다.");
    }

    // 옵션 그룹 전체 삭제
    @DeleteMapping("/{menuId}/options")
    public ResponseEntity<?> deleteAllOptionGroups(
            Authentication authentication,
            @PathVariable Long storeId,
            @PathVariable Long menuId) {
        menusService.deleteAllOptionGroups(menuId, authentication, storeId);
        return ResponseEntity.ok("옵션 그룹이 전체 삭제되었습니다.");
    }

    // 옵션 선택지 추가
    @PostMapping("/{menuId}/options/{optionGroupId}/choices")
    public ResponseEntity<?> addOptionChoice(
            Authentication authentication,
            @PathVariable Long storeId,
            @PathVariable Long menuId,
            @PathVariable Long optionGroupId,
            @RequestBody MenuOptionChoicesRequest request) {
        MenusResponse choice = menusService.addOptionChoice(menuId, optionGroupId, request, authentication, storeId);
        return ResponseEntity.status(HttpStatus.CREATED).body(choice);
    }

    // 옵션 선택지 수정
    @PatchMapping("/{menuId}/options/{optionGroupId}/choices/{choiceId}")
    public ResponseEntity<?> updateOptionChoice(
            Authentication authentication,
            @PathVariable Long storeId,
            @PathVariable Long menuId,
            @PathVariable Long optionGroupId,
            @PathVariable Long choiceId,
            @RequestBody MenuOptionChoicesRequest request) {
        MenusResponse choice = menusService.updateOptionChoice(menuId, optionGroupId, choiceId, request, authentication, storeId);
        return ResponseEntity.ok(choice);
    }

    // 옵션 선택지 단건 삭제
    @DeleteMapping("/{menuId}/options/{optionGroupId}/choices/{choiceId}")
    public ResponseEntity<?> deleteOptionChoice(
            Authentication authentication,
            @PathVariable Long storeId,
            @PathVariable Long menuId,
            @PathVariable Long optionGroupId,
            @PathVariable Long choiceId) {
        menusService.deleteOptionChoice(menuId, optionGroupId, choiceId, authentication, storeId);
        return ResponseEntity.ok("옵션 선택지가 삭제되었습니다.");
    }
}
