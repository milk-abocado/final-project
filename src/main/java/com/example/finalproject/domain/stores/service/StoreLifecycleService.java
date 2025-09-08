package com.example.finalproject.domain.stores.service;

import com.example.finalproject.domain.stores.auth.Role;
import com.example.finalproject.domain.stores.auth.SecurityUtil;
import com.example.finalproject.domain.stores.entity.Stores;
import com.example.finalproject.domain.stores.exception.ApiException;
import com.example.finalproject.domain.stores.exception.ErrorCode;
import com.example.finalproject.domain.stores.repository.StoresRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StoreLifecycleService {

    private final StoresRepository storesRepository;
    private final SecurityUtil security;

    /**
     * 가게 폐업 (논리 삭제)
     *
     * @param storeId 폐업 처리할 가게 ID
     * @throws ApiException
     *  - OWNER 권한이 아닐 경우 (FORBIDDEN)
     *  - 가게가 존재하지 않는 경우 (NOT_FOUND)
     *  - 다른 사용자의 가게를 폐업하려는 경우 (FORBIDDEN)
     */
    @Transactional
    public void retire(Long storeId) {
        Long uid = security.currentUserId();

        // 권한 확인 (OWNER만 가능)
        if (security.currentRole() != Role.OWNER)
            throw new ApiException(ErrorCode.FORBIDDEN, "가게 폐업은 OWNER만 가능합니다.");

        // 가게 조회
        Stores s = storesRepository.findById(storeId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "존재하지 않는 가게입니다."));

        // 본인 가게 여부 확인
        if (!s.getOwner().getId().equals(uid))
            throw new ApiException(ErrorCode.FORBIDDEN, "본인 가게만 폐업할 수 있습니다.");

        // 이미 폐업된 가게면 멱등 처리
        if (Boolean.FALSE.equals(s.getActive())) {
            return;
        }

        // 상태 전환 (폐업 처리)
        s.setActive(false);
        s.setRetiredAt(java.time.LocalDateTime.now());

        // 명시적 저장
        storesRepository.save(s);
    }
}
