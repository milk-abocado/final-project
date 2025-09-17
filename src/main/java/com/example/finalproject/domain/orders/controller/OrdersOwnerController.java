package com.example.finalproject.domain.orders.controller;

import com.example.finalproject.domain.orders.dto.response.OrdersResponse;
import com.example.finalproject.domain.orders.service.OrdersService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/owners")
@RequiredArgsConstructor
public class OrdersOwnerController {
    private final OrdersService ordersService;

    private ResponseEntity<String> str(HttpStatusCode status, String msg) {
        return ResponseEntity.status(status).body(msg);
    }

    // 주문 조회 (해당 가게에서 주문받은 목록)
    @GetMapping("/orders/stores/{storeId}")
    public ResponseEntity<?> getOrdersByStore(
            @PathVariable Long storeId,
            @RequestParam int page,
            @RequestParam int size
    ) {
        try{
            Pageable pageable = PageRequest.of(page, size);
            List<OrdersResponse> resp = ordersService.getOrdersByStore(storeId, pageable);
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            return str(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return str(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
        }
    }
}
