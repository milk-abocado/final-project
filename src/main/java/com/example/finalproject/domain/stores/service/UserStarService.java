package com.example.finalproject.domain.stores.service;

import com.example.finalproject.domain.stores.dto.response.StarredStoreResponse;
import com.example.finalproject.domain.stores.entity.Stores;
import com.example.finalproject.domain.stores.entity.UserStar;
import com.example.finalproject.domain.stores.exception.StoresApiException;
import com.example.finalproject.domain.stores.exception.StoresErrorCode;
import com.example.finalproject.domain.stores.repository.StoresRepository;
import com.example.finalproject.domain.stores.repository.UserStarRepository;
import com.example.finalproject.domain.users.entity.Users;
import com.example.finalproject.domain.users.repository.UsersRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * UserStarService
 * -------------------------------------------------
 * - 사용자 즐겨찾기(가게 찜) 도메인 서비스
 * - 추가/삭제/목록 조회 제공
 * - 현재 로그인 사용자(SecurityUtil 기반)에게만 허용
 */
@Service
@RequiredArgsConstructor
public class UserStarService {

    /** 사용자별 즐겨찾기 허용 최대 개수 */
    private static final int MAX_FAVORITES = 10;

    private final UserStarRepository starRepo;
    private final StoresRepository storesRepo;
    private final UsersRepository usersRepo;

    /**
     * 즐겨찾기 등록
     * 1) 로그인 사용자 확인
     * 2) 중복 등록 방지
     * 3) 개수 상한 검증
     * 4) 엔티티 로드 후 UserStar 저장
     *
     * @param storeId 즐겨찾기할 가게 ID
     * @return 사용자 메시지(가게명 포함)
     */
    @Transactional
    public String add(Long storeId) {
        Long uid = currentUserIdOrThrow();

        // 이미 즐겨찾기한 가게인지 검증
        if (starRepo.existsByUser_IdAndStore_Id(uid, storeId)) {
            throw new StoresApiException(StoresErrorCode.CONFLICT, "이미 즐겨찾기한 가게입니다.");
        }

        // 상한 체크
        long count = starRepo.countByUser_Id(uid);
        if (count >= MAX_FAVORITES) {
            throw new StoresApiException(StoresErrorCode.BAD_REQUEST, "즐겨찾기는 최대 " + MAX_FAVORITES + "개까지 가능합니다.");
        }

        // 연관 엔티티 로드
        Stores store = storesRepo.findById(storeId)
                .orElseThrow(() -> new StoresApiException(StoresErrorCode.NOT_FOUND, "존재하지 않는 가게입니다."));

        // 가게가 폐업 상태인지 확인
        if (!store.isActive()) { // 'isActive()' 메서드가 가게가 폐업했는지 확인
            throw new StoresApiException(StoresErrorCode.FORBIDDEN, "폐업한 가게는 즐겨찾기할 수 없습니다.");
        }

        Users user = usersRepo.findById(uid)
                .orElseThrow(() -> new StoresApiException(StoresErrorCode.NOT_FOUND, "존재하지 않는 사용자입니다."));

        // 저장
        starRepo.save(UserStar.builder().user(user).store(store).build());

        // 간단 응답 메시지
        return store.getName() + "을(를) 즐겨찾기 등록했습니다.";
    }

    /**
     * 즐겨찾기 삭제
     * 1) 로그인 사용자 확인
     * 2) 사용자-가게 조합으로 UserStar 조회
     * 3) 삭제
     *
     * @param storeId 즐겨찾기 해제할 가게 ID
     * @return 사용자 메시지(가게명 포함)
     */
    @Transactional
    public String remove(Long storeId) {
        Long uid = currentUserIdOrThrow();
        UserStar star = starRepo.findByUser_IdAndStore_Id(uid, storeId)
                .orElseThrow(() -> new StoresApiException(StoresErrorCode.NOT_FOUND, "즐겨찾기된 내역이 없습니다."));

        String storeName = star.getStore().getName();
        starRepo.delete(star);

        return storeName + "을(를) 즐겨찾기 삭제했습니다.";
    }

    /**
     * 즐겨찾기 목록 조회
     * - 기본은 전체를 등록 순(createdAt ASC)으로 반환
     * - onlyTop10=true면 최대 10개만 반환
     *
     * @param onlyTop10 최대 10개만 조회할지 여부
     * @return 가게 요약 응답 DTO 리스트
     */
    @Transactional
    public List<StarredStoreResponse> list(boolean onlyTop10) {
        Long uid = currentUserIdOrThrow();

        var stars = onlyTop10
                ? starRepo.findTop10ByUser_IdAndStore_ActiveTrueOrderByCreatedAtAsc(uid)
                : starRepo.findAllByUser_IdAndStore_ActiveTrueOrderByCreatedAtAsc(uid);

        // 엔티티 → DTO 매핑
        return stars.stream().map(s -> StarredStoreResponse.builder()
                .storeId(s.getStore().getId())
                .storeName(s.getStore().getName())
                .starredAt(s.getCreatedAt())
                .build()).toList();
    }

    /**
     * 현재 로그인한 사용자의 ID 반환
     * - 인증 없음/잘못됨 → 401 Unauthorized
     * - 사용자 미존재 → 401 Unauthorized
     * - 권한이 ROLE_USER가 아니면 → 403 Forbidden
     */
    private Long currentUserIdOrThrow() {
        // SecurityContext 에서 현재 인증(Authentication) 객체를 가져옴
        var auth = SecurityContextHolder.getContext().getAuthentication();

        // 인증 객체가 없거나(is null), 인증이 안 되었거나, 사용자 이름이 비어 있으면 → 401 Unauthorized
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            throw new StoresApiException(StoresErrorCode.UNAUTHORIZED, "인증이 필요합니다.");
        }

        // 인증 객체에서 사용자 이름(여기서는 email)을 꺼냄
        String email = auth.getName();

        // email 기반으로 Users 엔티티 조회, 없으면 → 401 Unauthorized
        Users user = usersRepo.findByEmail(email)
                .orElseThrow(() -> new StoresApiException(StoresErrorCode.UNAUTHORIZED, "사용자를 찾을 수 없습니다."));

        // 현재 사용자의 권한 목록에서 "USER" 권한이 있는지 확인
        boolean isUser = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("USER"::equals);

        // USER 권한이 없으면 → 403 Forbidden
        if (!isUser) {
            throw new StoresApiException(StoresErrorCode.FORBIDDEN, "즐겨찾기 기능은 USER만 가능합니다.");
        }

        // 모든 검증을 통과하면 사용자 ID 반환
        return user.getId();
    }
}
