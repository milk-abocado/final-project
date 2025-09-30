package com.example.finalproject.domain.stores.service;

import com.example.finalproject.domain.stores.entity.Stores;
import com.example.finalproject.domain.stores.exception.StoresApiException;
import com.example.finalproject.domain.stores.exception.StoresErrorCode;
import com.example.finalproject.domain.stores.repository.StoresRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StoreLifecycleService {

    private final StoresRepository storesRepository;

    /**
     * 가게 폐업 (논리 삭제)
     *
     * @param storeId 폐업 처리할 가게 ID
     * @return 폐업 처리된 가게 이름
     * @throws StoresApiException - OWNER 권한이 아닐 경우 (FORBIDDEN)
     *                      - 가게가 존재하지 않는 경우 (NOT_FOUND)
     *                      - 다른 사용자의 가게를 폐업하려는 경우 (FORBIDDEN)
     *                      - 이미 폐업한 경우 (GONE)
     */
    @Transactional
    public String retire(Long storeId) {
        // 0) 인증 가드: 인증 객체 확인
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
            throw new StoresApiException(StoresErrorCode.UNAUTHORIZED, "인증이 필요합니다.");
        }

        // 1) 권한 체크: OWNER 권한 보유 여부 확인
        boolean hasOwner =
                authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)   // 권한 문자열 추출
                        .filter(Objects::nonNull)              // null 제거
                        .map(String::trim)                     // 공백 제거
                        .anyMatch(a -> a.equals("ROLE_OWNER") || a.equals("OWNER"));
        // ROLE_OWNER 혹은 OWNER 권한 보유 여부 검사

        if (!hasOwner) {
            throw new StoresApiException(StoresErrorCode.FORBIDDEN, "가게 폐업은 OWNER만 가능합니다.");
        }

        // 2) 현재 사용자 식별자(email) 추출
        Object principal = authentication.getPrincipal();
        String email;
        if (principal instanceof UserDetails ud) {
            email = ud.getUsername();   // UserDetails 구현체에서 username 추출
        } else if (principal instanceof String s) {
            email = s;                  // 문자열이면 그대로 사용
        } else {
            throw new StoresApiException(StoresErrorCode.UNAUTHORIZED, "인증 정보를 확인할 수 없습니다.");
        }

        // 3) 가게 조회 (없는 경우 예외 발생)
        Stores s = storesRepository.findById(storeId)
                .orElseThrow(() -> new StoresApiException(StoresErrorCode.NOT_FOUND, "존재하지 않는 가게입니다."));

        // 4) 소유자 검증: 로그인한 사용자와 가게 소유자 일치 여부 확인
        if (!s.getOwner().getEmail().equals(email)) {
            throw new StoresApiException(StoresErrorCode.FORBIDDEN, "본인 가게만 폐업할 수 있습니다.");
        }

        // 5) 이미 폐업된 상태인지 확인
        if (s.getRetiredAt() != null || (s.getActive() != null && !s.getActive())) {
            // 이미 폐업된 경우 410(GONE) 상태 반환
            throw new StoresApiException(StoresErrorCode.GONE, "이미 폐업한 가게입니다.");
        }

        // 6) 논리 삭제 처리: 활성 상태 비활성화 및 폐업일자 기록
        s.setActive(false);
        s.setRetiredAt(LocalDateTime.now());

        // 7) 폐업 처리된 가게 이름 반환
        return s.getName();
    }
}
