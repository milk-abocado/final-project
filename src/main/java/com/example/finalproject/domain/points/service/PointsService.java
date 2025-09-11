package com.example.finalproject.domain.points.service;

import com.example.finalproject.domain.points.dto.PointsDtos;
import com.example.finalproject.domain.points.entity.Points;
import com.example.finalproject.domain.points.exception.PointException;
import com.example.finalproject.domain.points.repository.PointsRepository;
import com.example.finalproject.domain.users.Users;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointsService {

    private final PointsRepository pointsRepository;

    //포인트 적립
    @Transactional
    public PointsDtos.PointResponse earnPoints(Users user, PointsDtos.EarnRequest request) {
        if (request.getAmount() <= 0) {
            throw new PointException("포인트 적립 금액은 0보다 커야 합니다.");
        }

        Points points = new Points(user, request.getAmount(), request.getReason());
        Points saved = pointsRepository.save(points);

        PointsDtos.PointResponse response = new PointsDtos.PointResponse();
        response.setPointId(saved.getId());
        response.setUserId(user.getId());
        response.setAmount(saved.getAmount());
        response.setReason(saved.getReason());
        response.setCreatedAt(saved.getCreatedAt());

        return response;
    }

     // 포인트 사용
    @Transactional
    public PointsDtos.PointResponse usePoints(Users user, PointsDtos.UseRequest request) {
        int totalPoints = getTotalPoints(user.getId());

        if (request.getAmount() <= 0) {
            throw new PointException("포인트 사용 금액은 0보다 커야 합니다.");
        }
        if (totalPoints < request.getAmount()) {
            throw new PointException("보유 포인트가 부족합니다.");
        }

        Points points = new Points(user, -request.getAmount(), "포인트 사용");
        Points saved = pointsRepository.save(points);

        PointsDtos.PointResponse response = new PointsDtos.PointResponse();
        response.setPointId(saved.getId());
        response.setUserId(user.getId());
        response.setAmount(saved.getAmount());
        response.setReason(saved.getReason());
        response.setCreatedAt(saved.getCreatedAt());

        return response;
    }

    //유저 포인트 조회 (잔액, 내역)
    @Transactional
    public PointsDtos.UserPointsResponse getUserPoints(Long userId) {
        List<Points> history = pointsRepository.findByUserId(userId);

        int total = history.stream()
                .mapToInt(Points::getAmount)
                .sum();

        PointsDtos.UserPointsResponse response = new PointsDtos.UserPointsResponse();
        response.setUserId(userId);
        response.setTotalPoints(total);

        // 엔티티 → DTO 변환
        List<PointsDtos.PointResponse> responses = history.stream().map(p -> {
            PointsDtos.PointResponse dto = new PointsDtos.PointResponse();
            dto.setPointId(p.getId());
            dto.setUserId(p.getUser().getId());
            dto.setAmount(p.getAmount());
            dto.setReason(p.getReason());
            dto.setCreatedAt(p.getCreatedAt());
            return dto;
        }).toList();

        response.setHistory(responses);
        return response;
    }

    //총 포인트
    private int getTotalPoints(Long userId) {
        return pointsRepository.findByUserId(userId)
                .stream()
                .mapToInt(Points::getAmount)
                .sum();
    }
}
