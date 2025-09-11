package com.example.finalproject.domain.stores.controller;

import com.example.finalproject.domain.stores.dto.response.MessageResponse;
import com.example.finalproject.domain.stores.dto.response.StarredStoreResponse;
import com.example.finalproject.domain.stores.service.UserStarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users/me/stars")
public class UserStarController {

    private final UserStarService service;

    /**
     * 즐겨찾기 등록
     */
    @PostMapping("/{storeId}")
    public ResponseEntity<MessageResponse> add(@PathVariable Long storeId) {
        String msg = service.add(storeId);
        return ResponseEntity.ok(new MessageResponse(msg));
    }

    /**
     * 즐겨찾기 목록 (등록 순 정렬)
     * - 기본: 최대 10개만
     * - 모든 목록이 필요하면 ?all=true
     */
    @GetMapping
    public ResponseEntity<List<StarredStoreResponse>> list(
            @RequestParam(name = "all", defaultValue = "false") boolean all) {
        return ResponseEntity.ok(service.list(!all));
    }

    /**
     * 즐겨찾기 삭제
     */
    @DeleteMapping("/{storeId}")
    public ResponseEntity<MessageResponse> remove(@PathVariable Long storeId) {
        String msg = service.remove(storeId);
        return ResponseEntity.ok(new MessageResponse(msg));
    }
}
