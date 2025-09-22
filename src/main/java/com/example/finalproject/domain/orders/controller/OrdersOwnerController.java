package com.example.finalproject.domain.orders.controller;

import com.example.finalproject.domain.carts.exception.AccessDeniedException;
import com.example.finalproject.domain.orders.dto.response.OrdersResponse;
import com.example.finalproject.domain.orders.service.OrdersService;
import com.example.finalproject.domain.stores.entity.Stores;
import com.example.finalproject.domain.stores.repository.StoresRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/owners")
@RequiredArgsConstructor
public class OrdersOwnerController {

    private final OrdersService ordersService;
    private final StoresRepository storesRepository;

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
                .anyMatch(r -> r.equalsIgnoreCase("USER"))) {
            throw new AccessDeniedException("USER는 접근할 수 없습니다.");
        }

        return userId;

    }

    // 주문 조회 (해당 가게에서 주문받은 목록)
    @GetMapping("/orders/stores/{storeId}")
    public ResponseEntity<?> getOrdersByStore(
            Authentication authentication,
            @PathVariable Long storeId,
            @RequestParam int page,
            @RequestParam int size
    ) {
        try{
            // 권한 체크
            Long userId = verifiedUser(authentication);

            Stores store = storesRepository.findById(storeId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 가게입니다."));

            if (!store.getOwner().getId().equals(userId)) {
                throw new AccessDeniedException("본인 가게만 접근할 수 있습니다.");
            }

            Pageable pageable = PageRequest.of(page, size);
            List<OrdersResponse> resp = ordersService.getOrdersByStore(storeId, pageable);
            return ResponseEntity.ok(resp);

        } catch (AccessDeniedException e) {
            return str(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            return str(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return str(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
        }
    }
}
