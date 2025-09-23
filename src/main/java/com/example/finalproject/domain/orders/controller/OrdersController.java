package com.example.finalproject.domain.orders.controller;

import com.example.finalproject.domain.carts.exception.AccessDeniedException;
import com.example.finalproject.domain.orders.dto.request.OrderStatusRequest;
import com.example.finalproject.domain.orders.dto.request.OrdersRequest;
import com.example.finalproject.domain.orders.dto.response.OrderStatusResponse;
import com.example.finalproject.domain.orders.dto.response.OrdersResponse;
import com.example.finalproject.domain.orders.service.OrdersService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrdersController {

    private final OrdersService ordersService;

    private ResponseEntity<String> str(HttpStatusCode status, String msg) {
        return ResponseEntity.status(status).body(msg);
    }

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
            throw new AccessDeniedException("OWNER는 접근할 수 없습니다.");
        }

        return userId;

    }

    // 주문 생성
    @PostMapping
    public ResponseEntity<?> createOrder(
            Authentication authentication,
            @RequestBody OrdersRequest request) {
        try {
            // 권한 체크
            Long userId = verifiedUser(authentication);

            OrdersResponse resp = ordersService.createOrder(userId, request);

            return ResponseEntity.status(HttpStatus.CREATED).body(resp);

        } catch (AccessDeniedException e) {
            return str(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            return str(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return str(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
        }
    }

    // 주문 상태 수정
    @PatchMapping("/{orderId}")
    public ResponseEntity<?> updateStatus(
            Authentication authentication,
            @PathVariable Long orderId,
            @RequestBody OrderStatusRequest request) {
        try {

            OrderStatusResponse resp = ordersService.updateOrderStatus(authentication, orderId, request.getStatus());
            return ResponseEntity.ok(resp);

        } catch (AccessDeniedException e) {
            return str(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            return str(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return str(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
        }
    }

    // 주문 단건 조회
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(
            Authentication authentication,
            @PathVariable Long orderId) {
        try{

            OrdersResponse resp = ordersService.getOrder(authentication, orderId);
            return ResponseEntity.ok(resp);

        } catch (AccessDeniedException e) {
            return str(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            return str(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return str(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
        }
    }

    // 주문 내역 삭제
    @DeleteMapping("/{orderId}")
    public ResponseEntity<?> deleteOrder(
            Authentication authentication,
            @PathVariable Long orderId) {
        try{
            // 권한 체크
            Long userId = verifiedUser(authentication);

            ordersService.deleteOrder(userId, orderId);

            return ResponseEntity.ok().build();

        } catch (AccessDeniedException e) {
            return str(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            return str(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return str(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
        }
    }
}
