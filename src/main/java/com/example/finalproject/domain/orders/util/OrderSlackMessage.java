package com.example.finalproject.domain.orders.util;

public class OrderSlackMessage {
    public static String of(String status) {
        return switch (status) {
            case "WAITING"    -> "[사용자 알림] 주문 접수 대기 중입니다.";
            case "ACCEPTED"   -> "[사용자 알림] 주문이 접수되었습니다.";
            case "COOKING"    -> "[사용자 알림] 조리가 시작되었습니다.";
            case "DELIVERING" -> "[사용자 알림] 배달이 시작되었습니다.";
            case "COMPLETED"  -> "[사용자 알림] 배달이 완료되었습니다.";
            case "REJECTED"   -> "[사용자 알림] 주문이 거절되었습니다.";
            case "CANCELED"   -> "[사용자 알림] 주문이 취소되었습니다.";
            default           -> "[사용자 알림] 주문 상태가 변경되었습니다.";
        };
    }
}
