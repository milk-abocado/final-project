package com.example.finalproject.domain.stores.auth;

import com.example.finalproject.domain.stores.exception.ApiException;
import org.springframework.stereotype.Component;

/**
 * SecurityUtil
 *
 * - ThreadLocal을 사용하여 현재 요청(request) 단위의 사용자 정보를 보관하는 유틸 클래스
 * - 주로 인증/인가 과정에서 현재 로그인한 사용자의 ID와 권한(Role)을 관리함
 */
@Component
public class SecurityUtil {
    // 현재 요청 스레드에 사용자 ID 보관
    private static final ThreadLocal<Long> UID = new ThreadLocal<>();
    // 현재 요청 스레드에 사용자 Role 보관
    private static final ThreadLocal<Role> ROLE = new ThreadLocal<>();

    /**
     * 현재 로그인한 사용자의 ID 조회
     * @return 사용자 ID
     * @throws ApiException 로그인되지 않은 경우
     */
    public Long currentUserId() {
        Long id = UID.get();
        if (id == null) throw new com.example.finalproject.domain.stores.exception.ApiException(
                com.example.finalproject.domain.stores.exception.ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        return id;
    }

    /**
     * 현재 로그인한 사용자의 권한(Role) 조회
     * @return 사용자 권한
     * @throws ApiException 로그인되지 않은 경우
     */
    public Role currentRole() {
        Role r = ROLE.get();
        if (r == null) throw new com.example.finalproject.domain.stores.exception.ApiException(
                com.example.finalproject.domain.stores.exception.ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        return r;
    }

    /**
     * 테스트/임시 인증용 세터
     * @param id   사용자 ID
     * @param role 사용자 권한(Role)
     */
    public void set(Long id, Role role) { UID.set(id); ROLE.set(role); }
}
