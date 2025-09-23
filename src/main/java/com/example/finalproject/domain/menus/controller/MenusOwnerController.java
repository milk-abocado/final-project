package com.example.finalproject.domain.menus.controller;


import com.example.finalproject.domain.carts.exception.AccessDeniedException;
import com.example.finalproject.domain.menus.dto.request.MenuOptionChoicesRequest;
import com.example.finalproject.domain.menus.dto.request.MenuOptionsRequest;
import com.example.finalproject.domain.menus.dto.request.MenusRequest;
import com.example.finalproject.domain.menus.dto.response.MenusResponse;
import com.example.finalproject.domain.menus.service.MenusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/owners/stores/{storeId}/menus")
public class MenusOwnerController {

    private final MenusService menusService;

    private ResponseEntity<String> str(HttpStatusCode status, String msg) {
        return ResponseEntity.status(status).body(msg);
    }

    // 메뉴 생성
    @PostMapping
    public ResponseEntity<?> createMenu(
            Authentication authentication,
            @PathVariable Long storeId,
            @RequestBody MenusRequest request) {
        try {
            MenusResponse menus = menusService.createMenu(authentication, storeId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(menus);
        } catch (AccessDeniedException e) {
            return str(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            return str(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return str(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
        }

    }

    // 메뉴 수정
    @PatchMapping("/{menuId}")
    public ResponseEntity<?> updateMenu(
            Authentication authentication,
            @PathVariable Long storeId,
            @PathVariable Long menuId,
            @RequestBody MenusRequest request) {
        try {

            MenusResponse menus = menusService.updateMenu(authentication, menuId, storeId, request);
            return ResponseEntity.ok(menus);

        } catch (AccessDeniedException e) {
            return str(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            return str(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return str(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
        }
    }

    // 메뉴 삭제
    @DeleteMapping("/{menuId}")
    public ResponseEntity<?> deleteMenu(
            Authentication authentication,
            @PathVariable Long storeId,
            @PathVariable Long menuId) {
        try {

            menusService.deleteMenu(authentication, menuId, storeId);
            return ResponseEntity.ok("메뉴가 삭제되었습니다.");

        } catch (AccessDeniedException e) {
            return str(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            return str(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return str(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
        }

    }

    // 카테고리 단건 추가
    @PostMapping("/{menuId}/categories")
    public ResponseEntity<?> addCategory(
            Authentication authentication,
            @PathVariable Long storeId,
            @PathVariable Long menuId,
            @RequestBody String categoryName) {
        try {
            MenusResponse category = menusService.addCategory(menuId, categoryName, authentication, storeId);
            return ResponseEntity.status(HttpStatus.CREATED).body(category);
        } catch (AccessDeniedException e) {
            return str(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            return str(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return str(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
        }
    }

    // 카테고리 단건 수정
    @PatchMapping("/{menuId}/categories/{categoryId}")
    public ResponseEntity<?> updateCategory(
            Authentication authentication,
            @PathVariable Long storeId,
            @PathVariable Long menuId,
            @PathVariable Long categoryId,
            @RequestBody String newCategoryName) {
        try {
            MenusResponse category = menusService.updateCategory(menuId, categoryId, newCategoryName, authentication, storeId);
            return ResponseEntity.ok(category);
        } catch (AccessDeniedException e) {
            return str(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            return str(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return str(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
        }
    }

    // 카테고리 단건 삭제
    @DeleteMapping("/{menuId}/categories/{categoryId}")
    public ResponseEntity<?> deleteCategory(
            Authentication authentication,
            @PathVariable Long storeId,
            @PathVariable Long menuId,
            @PathVariable Long categoryId) {
        try {
            menusService.deleteCategory(menuId, categoryId, authentication, storeId);
            return ResponseEntity.ok("카테고리가 삭제되었습니다.");
        } catch (AccessDeniedException e) {
            return str(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            return str(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return str(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
        }
    }

    // 카테고리 전체 삭제
    @DeleteMapping("/{menuId}/categories")
    public ResponseEntity<?> deleteAllCategories(
            Authentication authentication,
            @PathVariable Long storeId,
            @PathVariable Long menuId) {
        try {
            menusService.deleteAllCategories(menuId, authentication, storeId);
            return ResponseEntity.ok("카테고리가 전체 삭제되었습니다.");
        } catch (AccessDeniedException e) {
            return str(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            return str(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return str(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
        }
    }

    // 메뉴 옵션 단건 생성 (옵션 그룹 + 선택지)
    @PostMapping("/{menuId}/options")
    public ResponseEntity<?> createOptionGroup(
            Authentication authentication,
            @PathVariable Long storeId,
            @PathVariable Long menuId,
            @RequestBody MenuOptionsRequest request) {
        try {
            MenusResponse option = menusService.createOptionGroup(menuId, request, authentication, storeId);
            return ResponseEntity.status(HttpStatus.CREATED).body(option);
        } catch (AccessDeniedException e) {
            return str(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            return str(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return str(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
        }
    }

    // 옵션 그룹 단건 삭제
    @DeleteMapping("/{menuId}/options/{optionGroupId}")
    public ResponseEntity<?> deleteOptionGroup(
            Authentication authentication,
            @PathVariable Long storeId,
            @PathVariable Long menuId,
            @PathVariable Long optionGroupId) {
        try {
            menusService.deleteOptionGroup(menuId, optionGroupId, authentication, storeId);
            return ResponseEntity.ok("옵션 그룹이 삭제되었습니다.");
        } catch (AccessDeniedException e) {
            return str(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            return str(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return str(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
        }
    }

    // 옵션 그룹 전체 삭제
    @DeleteMapping("/{menuId}/options")
    public ResponseEntity<?> deleteAllOptionGroups(
            Authentication authentication,
            @PathVariable Long storeId,
            @PathVariable Long menuId) {
        try {
            menusService.deleteAllOptionGroups(menuId, authentication, storeId);
            return ResponseEntity.ok("옵션 그룹이 전체 삭제되었습니다.");
        } catch (AccessDeniedException e) {
            return str(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            return str(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return str(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
        }
    }

    // 옵션 선택지 추가
    @PostMapping("/{menuId}/options/{optionGroupId}/choices")
    public ResponseEntity<?> addOptionChoice(
            Authentication authentication,
            @PathVariable Long storeId,
            @PathVariable Long menuId,
            @PathVariable Long optionGroupId,
            @RequestBody MenuOptionChoicesRequest request) {
        try {
            MenusResponse choice = menusService.addOptionChoice(menuId, optionGroupId, request, authentication, storeId);
            return ResponseEntity.status(HttpStatus.CREATED).body(choice);
        } catch (AccessDeniedException e) {
            return str(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            return str(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return str(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
        }
    }

    @PatchMapping("/{menuId}/options/{optionGroupId}/choices/{choiceId}")
    public ResponseEntity<?> updateOptionChoice(
            Authentication authentication,
            @PathVariable Long storeId,
            @PathVariable Long menuId,
            @PathVariable Long optionGroupId,
            @PathVariable Long choiceId,
            @RequestBody MenuOptionChoicesRequest request) {
        try {
            MenusResponse choice = menusService.updateOptionChoice(menuId, optionGroupId, choiceId, request, authentication, storeId);
            return ResponseEntity.ok(choice);
        } catch (AccessDeniedException e) {
            return str(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            return str(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return str(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
        }
    }

    @DeleteMapping("/{menuId}/options/{optionGroupId}/choices/{choiceId}")
    public ResponseEntity<?> deleteOptionChoice(
            Authentication authentication,
            @PathVariable Long storeId,
            @PathVariable Long menuId,
            @PathVariable Long optionGroupId,
            @PathVariable Long choiceId) {
        try {
            menusService.deleteOptionChoice(menuId, optionGroupId, choiceId, authentication, storeId);
            return ResponseEntity.ok("옵션 선택지가 삭제되었습니다.");
        } catch (AccessDeniedException e) {
            return str(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            return str(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return str(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
        }
    }

}
