package com.example.finalproject.domain.carts.controller;

import com.example.finalproject.domain.carts.dto.request.CartsItemRequest;
import com.example.finalproject.domain.carts.dto.response.CartsItemResponse;
import com.example.finalproject.domain.carts.dto.response.CartsResponse;
import com.example.finalproject.domain.carts.exception.AccessDeniedException;
import com.example.finalproject.domain.carts.service.CartsService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/carts")
@RequiredArgsConstructor
public class CartsController {

    private final CartsService cartsService;

    private Long verifiedUser(Authentication authentication) {

        // 로그인한 사용자의 userId 가져오기
        Long userId = Long.valueOf(
                ((Map<String, Object>) authentication.getDetails()).get("uid").toString()
        );

        // OWNER 권한 접근 방지 (대소문자 무시)
        if (authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r) // ROLE_ 제거
                .anyMatch(r -> r.equalsIgnoreCase("OWNER"))) {
            throw new AccessDeniedException("OWNER는 장바구니에 접근할 수 없습니다.");
        }

        return userId;

    }

    @GetMapping
    public ResponseEntity<?> getCart(Authentication authentication) {
        // 권한 체크
        Long userId = verifiedUser(authentication);

        // Redis에서 장바구니 조회
        CartsResponse cart = cartsService.getCart(userId);

        // 장바구니가 없으면 새로 생성
        if (cart == null) {
            cart = new CartsResponse();
            cart.setUserId(userId);
            cart.setUpdatedAt(LocalDateTime.now());
        }

        return ResponseEntity.ok(cart);
    }

    @PostMapping("/items")
    public ResponseEntity<?> addItem(
            Authentication authentication,
            @RequestBody CartsItemRequest cartsItemRequest) {
        // 권한 체크
        Long userId = verifiedUser(authentication);

        CartsItemResponse added = cartsService.addCartItem(userId, cartsItemRequest);
        return ResponseEntity.status(201).body(added);
    }

    @PatchMapping("/items/{cartItemId}")
    public ResponseEntity<?> updateItem(
            Authentication authentication,
            @PathVariable String cartItemId,
            @RequestBody CartsItemRequest request) {
        // 권한 체크
        Long userId = verifiedUser(authentication);

        CartsItemResponse updated = cartsService.updateCartItem(userId, cartItemId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<?> deleteItem(
            Authentication authentication,
            @PathVariable String cartItemId) {

        // 권한 체크
        Long userId = verifiedUser(authentication);

        cartsService.deleteCartItem(userId, cartItemId);
        return ResponseEntity.ok("장바구니 상품이 삭제되었습니다.");

    }

    @DeleteMapping
    public ResponseEntity<?> clearCart(Authentication authentication) {
        // 권한 체크
        Long userId = verifiedUser(authentication);

        cartsService.clearCart(userId);
        return ResponseEntity.ok("장바구니가 비워졌습니다.");
    }

}
