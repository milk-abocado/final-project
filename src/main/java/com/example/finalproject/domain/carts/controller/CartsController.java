package com.example.finalproject.domain.carts.controller;

import com.example.finalproject.domain.carts.dto.request.CartsItemRequest;
import com.example.finalproject.domain.carts.dto.response.CartsItemResponse;
import com.example.finalproject.domain.carts.dto.response.CartsResponse;
import com.example.finalproject.domain.carts.exception.CartAccessDeniedException;
import com.example.finalproject.domain.carts.service.CartsService;

import com.example.finalproject.domain.users.entity.Users;
import com.example.finalproject.domain.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/carts")
@RequiredArgsConstructor
public class CartsController {

    private final CartsService cartsService;
    private final UsersRepository usersRepository;

    private ResponseEntity<String> str(HttpStatusCode status, String msg) {
        return ResponseEntity.status(status).body(msg);
    }

    private void verifiedUser(Long userId) {
        Users user = usersRepository.findById(userId) // 지금은 없는 userId를 넣었을 때 예외 처리. 나중에 합칠 때 로그인 코드에 맞춰 수정 예정
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));

        if (user.getRole() == Users.Role.OWNER) {
            throw new CartAccessDeniedException("OWNER는 장바구니에 접근할 수 없습니다.");
        }
    }

    @GetMapping
    public ResponseEntity<?> getCart(@RequestHeader Long userId) {
        try {
            verifiedUser(userId); // 로그인/권한 체크(OWNER 제외)

            // Redis에서 장바구니 조회
            CartsResponse cart = cartsService.getCart(userId);

            if (cart == null) {
                cart = new CartsResponse();
                cart.setUserId(userId);
                cart.setUpdatedAt(LocalDateTime.now());
                return ResponseEntity.ok(cart);
            }

            return ResponseEntity.ok(cart);
        } catch (ResponseStatusException e) {
            return str(e.getStatusCode(), e.getReason());
        } catch (CartAccessDeniedException e) {
            return str(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            return str(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return str(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
        }
    }

    @PostMapping("/items")
    public ResponseEntity<?> addItem(
            @RequestHeader Long userId,
            @RequestBody CartsItemRequest cartsItemRequest) {

        try {
            verifiedUser(userId); // 로그인/권한 체크(OWNER 제외)

            CartsItemResponse added = cartsService.addCartItem(userId, cartsItemRequest);
            return ResponseEntity.status(201).body(added);
        } catch (ResponseStatusException e) {
            return str(e.getStatusCode(), e.getReason());
        } catch (CartAccessDeniedException e) {
            return str(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            return str(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (RuntimeException e) {
            return str(HttpStatus.CONFLICT, e.getMessage());
        } catch (Exception e) {
            return str(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
        }
    }

    @PatchMapping("/items/{cartItemId}")
    public ResponseEntity<?> updateItem(
            @RequestHeader Long userId,
            @PathVariable String cartItemId,
            @RequestBody CartsItemRequest request) {
        try {
            verifiedUser(userId); // 로그인/권한 체크(OWNER 제외)

            CartsItemResponse updated = cartsService.updateCartItem(userId, cartItemId, request);
            return ResponseEntity.ok(updated);
        } catch (ResponseStatusException e) {
            return str(e.getStatusCode(), e.getReason());
        } catch (CartAccessDeniedException e) {
            return str(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            return str(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return str(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
        }

    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<?> deleteItem(
            @RequestHeader Long userId,
            @PathVariable String cartItemId) {

        try {
            verifiedUser(userId); // 로그인/권한 체크(OWNER 제외)

            cartsService.deleteCartItem(userId, cartItemId);
            return ResponseEntity.ok("장바구니 상품이 삭제되었습니다.");

        } catch (ResponseStatusException e) {
            return str(e.getStatusCode(), e.getReason());
        } catch (CartAccessDeniedException e) {
            return str(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            return str(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return str(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
        }
    }

    @DeleteMapping
    public ResponseEntity<?> clearCart(@RequestHeader Long userId) {
        try {
            verifiedUser(userId); // 로그인/권한 체크(OWNER 제외)
            cartsService.clearCart(userId);
            return ResponseEntity.ok("장바구니가 비워졌습니다.");
        } catch (ResponseStatusException e) {
            return str(e.getStatusCode(), e.getReason());
        } catch (CartAccessDeniedException e) {
            return str(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            return str(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return str(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
        }
    }

}
