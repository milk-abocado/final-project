package com.example.finalproject.config;

import java.util.Optional;

public interface SessionIndexService {
    /** userId → sid 를 TTL과 함께 저장 */
    void set(Long userId, String sid, long ttlSeconds);

    /** 저장된 sid 의 TTL을 연장(갱신) */
    void bump(Long userId, long ttlSeconds);

    /** 저장된 sid 삭제 */
    void evict(Long userId);

    /** 저장된 sid 조회 */
    Optional<String> get(Long userId);

    /**
     * refreshToken 이 유효하며(서명/만료) 토큰의 uid/sid가
     * 저장된 값과 일치하는지 검사
     */
    boolean isRefreshValid(Long userId, String refreshToken);
}
