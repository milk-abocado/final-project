package com.example.finalproject.domain.points.controller;

import com.example.finalproject.domain.carts.exception.AccessDeniedException;
import com.example.finalproject.domain.points.dto.PointsDtos;
import com.example.finalproject.domain.points.exception.PointErrorCode;
import com.example.finalproject.domain.points.exception.PointException;
import com.example.finalproject.domain.points.service.PointsService;
import com.example.finalproject.domain.users.UserRole;
import com.example.finalproject.domain.users.entity.Users;
import com.example.finalproject.domain.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/points")
@RequiredArgsConstructor
public class PointsController {

    private final PointsService pointsService;
    private final UsersRepository usersRepository;

    private Long verifiedUser(Authentication authentication) {
        // 로그인한 사용자의 userId 가져오기
        Long userId = Long.valueOf(
                ((Map<String, Object>) authentication.getDetails()).get("uid").toString()
        );

        // OWNER 권한 접근 방지
        if (authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                .anyMatch(r -> r.equalsIgnoreCase("OWNER"))) {
            throw new AccessDeniedException("OWNER는 포인트에 접근할 수 없습니다.");
        }
        return userId;
    }

    private Long verifiedADMIN(Authentication authentication) {
        Long userId = Long.valueOf(
                ((Map<String, Object>) authentication.getDetails()).get("uid").toString()
        );

        // OWNER 접근 차단
        if (authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                .anyMatch(r -> r.equalsIgnoreCase("OWNER"))) {
            throw new AccessDeniedException("OWNER는 포인트에 접근할 수 없습니다.");
        }

        // USER 접근 차단
        if (authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                .anyMatch(r -> r.equalsIgnoreCase("USER"))) {
            throw new AccessDeniedException("USER는 접근할 수 없습니다.");
        }
        return userId;
    }

    // 포인트 적립 (관리자 전용)
    @PostMapping("/earn")
    public ResponseEntity<PointsDtos.PointResponse> earnPoints(
            @RequestBody PointsDtos.EarnRequest request,
            Authentication authentication
    ) {
        verifiedADMIN(authentication);
        Users user = usersRepository.findById(request.getUserId())
                .orElseThrow(() -> new PointException(PointErrorCode.USER_NOT_FOUND));

        PointsDtos.PointResponse response = pointsService.earnPoints(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 포인트 사용
    @PostMapping("/use")
    public ResponseEntity<PointsDtos.PointResponse> usePoints(
            @RequestBody PointsDtos.UseRequest request,
            Authentication authentication
    ) {
        verifiedUser(authentication);
        Users user = usersRepository.findById(request.getUserId())
                .orElseThrow(() -> new PointException(PointErrorCode.USER_NOT_FOUND));

        PointsDtos.PointResponse response = pointsService.usePoints(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 포인트 조회
    @GetMapping("/{userId}")
    public ResponseEntity<PointsDtos.UserPointsResponse> getUserPoints(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        Long checkUserId = verifiedUser(authentication);
        Users user = usersRepository.findById(checkUserId)
                .orElseThrow(() -> new PointException(PointErrorCode.USER_NOT_FOUND));

        if (user.getRole() == UserRole.USER && !user.getId().equals(userId)) {
            throw new AccessDeniedException("본인의 포인트만 조회할 수 있습니다.");
        }

        PointsDtos.UserPointsResponse response = pointsService.getUserPoints(userId);
        return ResponseEntity.ok(response);
    }
}
