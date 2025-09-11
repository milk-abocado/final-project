package com.example.finalproject.domain.points.controller;

import com.example.finalproject.domain.points.dto.PointsDtos;
import com.example.finalproject.domain.points.service.PointsService;
import com.example.finalproject.domain.users.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/points")
@RequiredArgsConstructor
public class PointsController {

    private final PointsService pointsService;

    // 포인트 적립
    @PostMapping("/earn")
    public ResponseEntity<PointsDtos.PointResponse> earnPoints(
            @RequestBody PointsDtos.EarnRequest request
    ) {
        //  임시: 더미 유저 생성 (추후 로그인 사용자로 교체)
        Users dummyUser = new Users();
        dummyUser.setId(request.getUserId());

        PointsDtos.PointResponse response = pointsService.earnPoints(dummyUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    //포인트 사용
    @PostMapping("/use")
    public ResponseEntity<PointsDtos.PointResponse> usePoints(
            @RequestBody PointsDtos.UseRequest request
    ) {
        Users dummyUser = new Users();
        dummyUser.setId(request.getUserId());

        PointsDtos.PointResponse response = pointsService.usePoints(dummyUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    //포인트 조회
    @GetMapping("/{userId}")
    public ResponseEntity<PointsDtos.UserPointsResponse> getUserPoints(
            @PathVariable Long userId
    ) {
        PointsDtos.UserPointsResponse response = pointsService.getUserPoints(userId);
        return ResponseEntity.ok(response);
    }
}
