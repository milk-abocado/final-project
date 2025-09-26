package com.example.finalproject.domain.stores.service;

import com.example.finalproject.domain.stores.entity.Stores;
import com.example.finalproject.domain.stores.exception.ApiException;
import com.example.finalproject.domain.stores.exception.ErrorCode;
import com.example.finalproject.domain.stores.repository.StoresRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StoreLifecycleService {

    private final StoresRepository storesRepository;

    /**
     * 가게 폐업 (논리 삭제)
     *
     * @param storeId 폐업 처리할 가게 ID
     * @throws ApiException - OWNER 권한이 아닐 경우 (FORBIDDEN)
     *                      - 가게가 존재하지 않는 경우 (NOT_FOUND)
     *                      - 다른 사용자의 가게를 폐업하려는 경우 (FORBIDDEN)
     */
    @Transactional
    public String retire(Long storeId) {
        // SecurityContextHolder를 사용하여 현재 사용자 ID 및 권한 확인
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // UserDetails 객체를 통해 실제 사용자 정보 추출
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();  // 이메일을 그대로 사용

        // 권한 확인 (OWNER만 가능) -> ROLE_ 접두사를 포함하여 권한 비교
        boolean isOwner = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("OWNER"));
        if (!isOwner) {
            throw new ApiException(ErrorCode.FORBIDDEN, "가게 폐업은 OWNER만 가능합니다.");
        }

        // 가게 조회
        Stores s = storesRepository.findById(storeId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "존재하지 않는 가게입니다."));

        // 본인 가게 여부 확인 (이메일로 비교)
        if (!s.getOwner().getEmail().equals(email)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "본인 가게만 폐업할 수 있습니다.");
        }

        // 이미 폐업된 가게면 예외
        if ((s.getRetiredAt() != null) || (s.getActive() != null && !s.getActive())) {
            throw new ApiException(ErrorCode.CONFLICT, "이미 폐업된 가게입니다.");
        }

        // 논리 삭제 처리
        s.setActive(false);
        s.setRetiredAt(LocalDateTime.now());

        return s.getName();
    }
}
