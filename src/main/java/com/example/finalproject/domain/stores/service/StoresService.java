package com.example.finalproject.domain.stores.service;

import com.example.finalproject.domain.stores.dto.request.StoresRequest;
import com.example.finalproject.domain.stores.dto.response.StoresResponse;
import com.example.finalproject.domain.stores.entity.Stores;
import com.example.finalproject.domain.stores.exception.StoresApiException;
import com.example.finalproject.domain.stores.exception.StoresErrorCode;
import com.example.finalproject.domain.stores.geo.GeocodingPort;
import com.example.finalproject.domain.stores.repository.StoresRepository;
import com.example.finalproject.domain.users.UserRole;
import com.example.finalproject.domain.users.entity.Users;
import com.example.finalproject.domain.users.repository.UsersRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.Collection;

/**
 * StoresService
 * -------------------------------------------------
 * - 가게 생성/수정 등 쓰기(Write) 로직 담당 서비스
 * - 권한 검증(OWNER), 입력값 검증, 주소 지오코딩, 비즈니스 제약(가게 수 제한) 수행
 */
@Service
@RequiredArgsConstructor
public class StoresService {

    private final StoresRepository storesRepository; // 가게 저장소
    private final UsersRepository usersRepository;   // 사용자 저장소 (OWNER 확인)
    private final GeocodingPort geocoding;           // 주소 → 좌표 변환 포트

    /**
     * 가게 생성
     * - OWNER 권한 필요
     * - 입력값 검증(영업시간/최소주문/주소 등)
     * - 주소 중복/가게 수 제한 검증
     * - 주소 지오코딩 필수(좌표 저장)
     */
    @Transactional
    public StoresResponse create(StoresRequest req) {
        // 1) 인증/권한 확인
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 인증 정보에서 Principal을 가져와서 UserDetails 객체로 캐스팅
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // 사용자 정보를 사용하여 이메일 가져오기
        String username = userDetails.getUsername();  // 여기서 email을 가져옴

        // 권한 확인
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String roleString = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse(null);

        UserRole currentRole = UserRole.valueOf(roleString.replace("ROLE_", "")); // "ROLE_" 제거 후 변환

        // OWNER 권한 확인
        if (currentRole != UserRole.OWNER) {
            throw new StoresApiException(StoresErrorCode.FORBIDDEN, "가게 생성은 OWNER만 가능합니다.");
        }

        // 2) OWNER 존재 검증 (이메일로 사용자 조회)
        Users owner = usersRepository.findByEmail(username)
                .orElseThrow(() -> new StoresApiException(StoresErrorCode.UNAUTHORIZED, "OWNER 계정이 존재하지 않습니다."));

        // 3) 공통 입력 검증 (자정 넘김 허용, 동일 시각 금지)
        validateCommon(req);

        // 4) 문자열 정리
        String name = req.getName().trim();
        String address = req.getAddress().trim();

        // 5) 주소 중복 검증 (운영 중 & 미폐업과만 충돌 금지)
        if (storesRepository.existsByAddressAndActiveTrueAndRetiredAtIsNull(address)) {
            throw new StoresApiException(StoresErrorCode.CONFLICT, "해당 주소로 운영 중인 가게가 이미 존재합니다.");
        }

        // 6) 활성 가게 수 제한 (예: 최대 3개)
        long activeCount = storesRepository.countByOwner_IdAndActiveTrue(owner.getId());
        if (activeCount >= 3) {
            throw new StoresApiException(StoresErrorCode.LIMIT_EXCEEDED, "OWNER의 운영 가게 수는 최대 3개입니다.");
        }

        // 7) 배달비 기본값/범위 검증
        int deliveryFee = (req.getDeliveryFee() == null) ? 0 : req.getDeliveryFee();
        if (deliveryFee < 0) throw new StoresApiException(StoresErrorCode.BAD_REQUEST, "배달비는 0원 이상이어야 합니다.");

        // 8) 주소 → 좌표 (필수: 실패 시 예외)
        var latLng = geocoding.geocode(address)
                .orElseThrow(() -> new StoresApiException(StoresErrorCode.BAD_REQUEST, "주소 지오코딩에 실패했습니다. 주소를 확인해 주세요."));

        // 9) 저장
        Stores saved = storesRepository.save(
                Stores.builder()
                        .owner(owner)
                        .name(name)
                        .address(address)
                        .latitude(latLng.getLat())
                        .longitude(latLng.getLng())
                        .minOrderPrice(req.getMinOrderPrice())
                        .opensAt(req.getOpensAt())
                        .closesAt(req.getClosesAt())
                        .deliveryFee(deliveryFee)
                        .active(true)
                        .build()
        );

        // 10) 응답 DTO 변환
        return toResponse(saved);
    }

    /**
     * 가게 수정
     * - OWNER 권한 및 본인 소유 가게만 가능
     * - 주소 변경 시에만 재지오코딩
     */
    @Transactional
    public StoresResponse update(Long storeId, StoresRequest req) {
        // 1) 권한 확인
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();  // Get email (username)
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // 권한 확인
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String roleString = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse(null);

        UserRole currentRole = UserRole.valueOf(roleString.replace("ROLE_", "")); // "ROLE_" 제거 후 변환
        if (currentRole != UserRole.OWNER) {
            throw new StoresApiException(StoresErrorCode.FORBIDDEN, "가게 수정은 OWNER만 가능합니다.");
        }

        // 2) 대상 가게 조회
        Stores store = storesRepository.findById(storeId)
                .orElseThrow(() -> new StoresApiException(StoresErrorCode.NOT_FOUND, "존재하지 않는 가게입니다."));

        // 3) 본인 소유 검증
        if (!store.getOwner().getEmail().equals(username)) {
            throw new StoresApiException(StoresErrorCode.FORBIDDEN, "본인이 소유한 가게만 수정할 수 있습니다.");
        }

        // 4) 공통 입력 검증
        validateCommon(req);

        // 5) 값 정리
        String newName = req.getName().trim();
        String newAddress = req.getAddress().trim();

        // 6) 주소 중복 검증(자기 자신 제외)
        if (storesRepository.existsByAddressAndActiveTrueAndRetiredAtIsNullAndIdNot(newAddress, storeId)) {
            throw new StoresApiException(StoresErrorCode.CONFLICT, "해당 주소로 운영 중인 가게가 이미 존재합니다.");
        }

        // 7) 배달비 검증
        int deliveryFee = (req.getDeliveryFee() == null) ? 0 : req.getDeliveryFee();
        if (deliveryFee < 0) throw new StoresApiException(StoresErrorCode.BAD_REQUEST, "배달비는 0원 이상이어야 합니다.");

        // 8) 주소 변경 여부 판단
        boolean addressChanged = !newAddress.equals(store.getAddress());

        // 9) 기본 필드 갱신
        store.setName(newName);
        store.setAddress(newAddress);
        store.setMinOrderPrice(req.getMinOrderPrice());
        store.setOpensAt(req.getOpensAt());
        store.setClosesAt(req.getClosesAt());
        store.setDeliveryFee(deliveryFee);

        // 10) 주소 변경 시 좌표 재계산
        if (addressChanged) {
            var latLng = geocoding.geocode(newAddress)
                    .orElseThrow(() -> new StoresApiException(StoresErrorCode.BAD_REQUEST, "주소 지오코딩에 실패했습니다."));
            store.setLatitude(latLng.getLat());
            store.setLongitude(latLng.getLng());
        }

        // 11) 응답 DTO 변환
        return toResponse(store);
    }

    /**
     * 공통 입력 검증
     * - 이름/주소/최소주문금액/영업시간 검증
     * - 자정 넘김(예: 18:00~02:00) 허용, 단 open==close 금지
     */
    private void validateCommon(StoresRequest req) {
        if (isBlank(req.getName())) {
            throw new StoresApiException(StoresErrorCode.BAD_REQUEST, "가게 이름은 필수입니다.");
        }
        if (isBlank(req.getAddress())) {
            throw new StoresApiException(StoresErrorCode.BAD_REQUEST, "주소는 필수입니다.");
        }
        if (req.getMinOrderPrice() == null || req.getMinOrderPrice() <= 0) {
            throw new StoresApiException(StoresErrorCode.BAD_REQUEST, "최소 주문 금액이 0원 이하이거나 음수입니다.");
        }

        LocalTime open = req.getOpensAt();
        LocalTime close = req.getClosesAt();
        if (open == null || close == null) {
            throw new StoresApiException(StoresErrorCode.BAD_REQUEST, "영업 시작/종료 시간은 필수입니다.");
        }
        // 오픈 시간이 마감 시간과 같거나 ‘마감보다 늦을’ 때 / 다만 동일 시각(open == close)은 금지
        if (open.equals(close) || open.isAfter(close)) {
            throw new StoresApiException(StoresErrorCode.BAD_REQUEST, "오픈 시간이 마감 시간보다 늦거나 동일할 수 없습니다.");
        }
    }

    /** 엔티티 → 응답 DTO 변환 */
    private StoresResponse toResponse(Stores s) {
        boolean openNow = isOpenNow(s.getOpensAt(), s.getClosesAt());
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
     * 현재 영업중 여부 계산
     * - 일반 케이스(open < close): open ≤ now ≤ close
     * - 자정 넘김(open > close):  now ≥ open OR now ≤ close
     */
    private boolean isOpenNow(LocalTime open, LocalTime close) {
        if (open == null || close == null) return false;
        LocalTime now = LocalTime.now();

        if (open.isBefore(close)) {
            return !now.isBefore(open) && !now.isAfter(close);
        } else {
            return !now.isBefore(open) || !now.isAfter(close);
        }
    }

    /** 공백/빈 문자열 판단 유틸 */
    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
