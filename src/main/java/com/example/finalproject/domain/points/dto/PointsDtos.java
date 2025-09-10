package com.example.finalproject.domain.points.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class PointsDtos {

    //포인트 적립
    @Getter
    @Setter
    public static class EarnRequest{
        private Long userId;
        private int amount;
        private String reason;
    }

    //포인트 사용 요청
    @Getter
    @Setter
    public static class UseRequest{
        private Long userId;
        private Long orderId; // 추후 주문 구현 시 DB 추가해야함
        private int amount;
    }

    // 포인트 기록
    @Getter
    @Setter
    public static class PointResponse{
        private Long pointId;
        private Long userId;
        private int amount;
        private String reason;
        private LocalDateTime createdAt;
    }

    //포인트 잔액, 내역
    @Getter
    @Setter
    public static class UserPointsResponse{
        private Long userId;
        private int totalPoints;
        private List<PointResponse> history;
    }
}
