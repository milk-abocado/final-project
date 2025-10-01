package com.example.finalproject.domain.elasticsearchpopular.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * SearchRecordRequest
 * 사용자가 검색을 수행할 때 요청 본문으로 전달되는 DTO
 * <p>
 * - 검색 키워드
 * - 검색 지역
 * - 사용자 ID
 * <p>
 * 컨트롤러(@RequestBody)에서 받아서 서비스 레이어로 전달됨
 */
@Getter
@Setter
public class SearchRecordRequest {

    /**
     * 검색 키워드
     * 사용자가 입력한 검색어
     */
    private String keyword;

    /**
     * 지역
     * 검색이 수행된 지역 (예: "서울", "부산")
     */
    private String region;

    /**
     * 사용자 ID
     * 검색을 수행한 사용자 식별자
     * - 로그인 사용자라면 userId 저장
     * - 비로그인 사용자라면 null일 수도 있음
     */
    private Long userId;
}
