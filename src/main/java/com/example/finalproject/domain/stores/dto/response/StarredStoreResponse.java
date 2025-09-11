package com.example.finalproject.domain.stores.dto.response;

import lombok.*;

import java.time.LocalDateTime;

/**
 * StarredStoreResponse
 * -------------------------------------------------
 * - 사용자가 즐겨찾기한 가게 정보를 응답으로 전달하기 위한 DTO
 * - storeId : 가게 ID
 * - storeName : 가게 이름
 * - starredAt : 즐겨찾기 등록 시각
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class StarredStoreResponse {
    private Long storeId;               // 가게 ID
    private String storeName;           // 가게 이름
    private LocalDateTime starredAt;    // 즐겨찾기 등록 시각
}
