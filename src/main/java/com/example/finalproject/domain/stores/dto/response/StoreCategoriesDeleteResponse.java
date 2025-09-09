package com.example.finalproject.domain.stores.dto.response;

import java.util.List;

/**
 * 카테고리 삭제 후 응답 DTO
 * - 삭제 결과 메시지와 현재 카테고리 상태를 함께 반환
 */
public record StoreCategoriesDeleteResponse(
        Long storeId,                   // 가게 ID
        List<String> categories, // 현재 남아있는 카테고리 목록
        String message                  // 처리 결과 메시지
) {}

