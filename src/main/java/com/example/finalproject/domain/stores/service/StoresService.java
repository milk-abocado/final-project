package com.example.finalproject.domain.stores.service;

import com.example.finalproject.domain.stores.auth.Role;
import com.example.finalproject.domain.stores.auth.SecurityUtil;
import com.example.finalproject.domain.stores.dto.request.StoresRequest;
import com.example.finalproject.domain.stores.dto.response.StoresResponse;
import com.example.finalproject.domain.stores.entity.Stores;
import com.example.finalproject.domain.stores.entity.Users;
import com.example.finalproject.domain.stores.exception.ApiException;
import com.example.finalproject.domain.stores.exception.ErrorCode;
import com.example.finalproject.domain.stores.repository.StoresRepository;
import com.example.finalproject.domain.stores.repository.UsersRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class StoresService {

    private final StoresRepository storesRepository;
    private final UsersRepository usersRepository;
    private final SecurityUtil security;

    @Transactional
    public StoresResponse create(StoresRequest req) {
        // 인증/권한
        Long ownerId = security.currentUserId();

        // 권한 검증 - OWNER만 허용 (USER/ADMIN 차단)
        if (security.currentRole() != Role.OWNER) {
            throw new ApiException(ErrorCode.FORBIDDEN, "가게 생성은 OWNER만 가능합니다.");
        }

        // 입력 검증
        if (isBlank(req.getName())) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "가게 이름은 필수입니다.");
        }
        if (isBlank(req.getAddress())) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "주소는 필수입니다.");
        }
        if (req.getMinOrderPrice() == null || req.getMinOrderPrice() <= 0) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "최소 주문 금액이 0원 이하이거나 음수입니다.");
        }

        LocalTime open = req.getOpensAt();
        LocalTime close = req.getClosesAt();
        if (open == null || close == null) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "영업 시작/종료 시간은 필수입니다.");
        }

        // open == close 금지(24시간 영업은 별도 정책으로), 나머지는 허용
        if (open.equals(close)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "오픈 시간과 마감 시간이 동일할 수 없습니다.");
        }

        // 중복/제한 검증
        // 주소 trim 선반영 후 중복 체크 & 저장 모두 동일 값 사용
        String name = req.getName().trim();
        String address = req.getAddress().trim();

        if (storesRepository.existsByAddress(address))
            throw new ApiException(ErrorCode.CONFLICT, "이미 등록된 주소입니다.");

        Users owner = usersRepository.findById(ownerId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "OWNER 계정이 존재하지 않습니다."));

        // 활성 가게 수 기준 제한
        long activeCount = storesRepository.countByOwner_IdAndActiveTrue(ownerId);
        if (activeCount >= 3)
            throw new ApiException(ErrorCode.LIMIT_EXCEEDED, "OWNER의 운영 가게 수는 최대 3개입니다.");

        Integer deliveryFee = (req.getDeliveryFee() == null) ? 0 : req.getDeliveryFee();
        if (deliveryFee < 0) throw new ApiException(ErrorCode.BAD_REQUEST, "배달비는 0원 이상이어야 합니다.");

        // 저장
        Stores saved = storesRepository.save(
                Stores.builder()
                        .owner(owner)
                        .name(name)
                        .address(address)
                        .minOrderPrice(req.getMinOrderPrice())
                        .opensAt(open)
                        .closesAt(close)
                        .deliveryFee(deliveryFee)
                        .active(true) // 기본 영업중
                        .build()
        );

        return toResponse(saved);
    }

    /** 가게 수정 */
    @Transactional
    public StoresResponse update(Long storeId, StoresRequest req) {
        // 인증/권한
        Long ownerId = security.currentUserId();
        if (security.currentRole() != Role.OWNER)
            throw new ApiException(ErrorCode.FORBIDDEN, "가게 수정은 OWNER만 가능합니다.");

        // 대상 가게 조회
        Stores store = storesRepository.findById(storeId)
                .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST, "존재하지 않는 가게입니다."));

        // 본인 가게인지 검증
        if (!store.getOwner().getId().equals(ownerId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "본인이 소유한 가게만 수정할 수 있습니다.");
        }

        // 입력 검증
        if (isBlank(req.getName())) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "가게 이름은 필수입니다.");
        }
        if (isBlank(req.getAddress())) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "주소는 필수입니다.");
        }
        if (req.getMinOrderPrice() == null || req.getMinOrderPrice() <= 0) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "최소 주문 금액이 0원 이하이거나 음수입니다.");
        }

        LocalTime open = req.getOpensAt();
        LocalTime close = req.getClosesAt();
        if (open == null || close == null) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "영업 시작/종료 시간은 필수입니다.");
        }

        // 수정 정책: open == close 또는 open >= close 금지 (자정 넘김 미허용)
        if (!open.isBefore(close)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "오픈 시간은 마감 시간보다 이르고, 동일할 수 없습니다.");
        }

        // 주소 중복(본인 현재 가게 제외)
        String address = req.getAddress().trim();
        if (storesRepository.existsByAddressAndIdNot(address, storeId))
            throw new ApiException(ErrorCode.CONFLICT, "이미 등록된 주소입니다.");

        Integer deliveryFee = (req.getDeliveryFee() == null) ? 0 : req.getDeliveryFee();
        if (deliveryFee < 0) throw new ApiException(ErrorCode.BAD_REQUEST, "배달비는 0원 이상이어야 합니다.");


        store.setName(req.getName().trim());
        store.setAddress(address);
        store.setMinOrderPrice(req.getMinOrderPrice());
        store.setOpensAt(open);
        store.setClosesAt(close);
        store.setDeliveryFee(deliveryFee);

        return toResponse(store);
    }

    private StoresResponse toResponse(Stores s) {
        boolean openNow = isOpenNow(s.getOpensAt(), s.getClosesAt()); // ★ CHANGED: 자정 넘김 지원
        return new StoresResponse(
                s.getId(),
                s.getOwner().getId(),
                s.getName(),
                s.getAddress(),
                s.getMinOrderPrice(),
                s.getOpensAt(),
                s.getClosesAt(),
                s.getDeliveryFee(),
                openNow,
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }

    /**
     * 자정 넘김(예: 18:00~02:00)과 경계값 포함을 지원하는 영업중 판정
     * - 포함형 비교: [open, close]
     * - 일반 구간(open < close): open <= now <= close
     * - 자정 넘김(open > close): now >= open OR now <= close
     */
    private boolean isOpenNow(LocalTime open, LocalTime close) { // ★ CHANGED
        if (open == null || close == null) return false;
        LocalTime now = LocalTime.now();

        if (open.isBefore(close)) {
            // 일반 케이스 (예: 09:00 ~ 18:00)
            return !now.isBefore(open) && !now.isAfter(close);
        } else {
            // 자정 넘김 케이스 (예: 18:00 ~ 02:00)
            return !now.isBefore(open) || !now.isAfter(close);
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
