package com.example.finalproject.domain.stores.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 공통 메시지 응답 DTO
 *
 * - 단순히 상태 메시지와 발생 시각을 내려줄 때 사용
 * - 예) 성공/실패 안내, 알림성 응답 등
 */
@Getter
@AllArgsConstructor
public class ApiMessageResponse {
    private final String message;           // 응답 메시지
    private final LocalDateTime timestamp;  // 응답 시각
}
