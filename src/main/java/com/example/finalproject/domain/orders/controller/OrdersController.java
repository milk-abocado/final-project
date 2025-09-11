package com.example.finalproject.domain.orders.controller;

import com.example.finalproject.domain.orders.dto.request.OrderStatusRequest;
import com.example.finalproject.domain.orders.dto.request.OrdersRequest;
import com.example.finalproject.domain.orders.dto.response.OrderStatusResponse;
import com.example.finalproject.domain.orders.dto.response.OrdersResponse;
import com.example.finalproject.domain.orders.service.OrdersService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrdersController {

    private final OrdersService ordersService;

    private ResponseEntity<String> str(HttpStatusCode status, String msg) {
        return ResponseEntity.status(status).body(msg);
    }

    // 접근 권한 및 로그인 확인 예외처리 제외하고 구현 <- 추후 로그인 기능과 merge 했을 때 합칠 예정
    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestHeader Long userId,
            @RequestBody OrdersRequest request
    ) {
        try {
            OrdersResponse resp = ordersService.createOrder(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(resp);
        } catch (IllegalArgumentException e) {
            return str(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return str(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
        }
    }

    @PatchMapping("/{orderId}")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long orderId,
            @RequestBody OrderStatusRequest request
    ) {
        try {
            OrderStatusResponse resp = ordersService.updateOrderStatus(orderId, request.getStatus());
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            return str(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return str(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable Long orderId) {
        try{
        OrdersResponse resp = ordersService.getOrder(orderId);
        return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            return str(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return str(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
        }
    }
}
