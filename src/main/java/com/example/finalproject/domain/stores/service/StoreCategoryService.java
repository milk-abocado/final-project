package com.example.finalproject.domain.stores.service;

import com.example.finalproject.domain.stores.category.StoreCategory;
import com.example.finalproject.domain.stores.dto.request.StoreCategoriesRequest;
import com.example.finalproject.domain.stores.dto.response.StoreCategoriesDeleteResponse;
import com.example.finalproject.domain.stores.dto.response.StoreCategoriesResponse;
import com.example.finalproject.domain.stores.entity.StoreCategoryLink;
import com.example.finalproject.domain.stores.entity.Stores;
import com.example.finalproject.domain.stores.exception.ApiException;
import com.example.finalproject.domain.stores.exception.ErrorCode;
import com.example.finalproject.domain.stores.repository.StoreCategoryLinkRepository;
import com.example.finalproject.domain.stores.repository.StoresRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreCategoryService {

    private final StoresRepository storesRepository;
    private final StoreCategoryLinkRepository linkRepository;

    /**
     * 인가 & 소유권 검증
     * - 인증 토큰이 유효한지 확인(미인증 시 401)
     * - 현재 사용자가 OWNER 권한인지 확인(권한 부족 시 403)
     * - 대상 가게 존재 여부 확인(없으면 404)
     * - 가게 소유자 이메일과 현재 사용자 이메일 일치 확인(불일치 시 403)
     *
     * @param storeId 대상 가게 ID
     * @return 검증 통과한 Stores 엔티티
     * @throws ApiException 401/403/404 상황별 예외
     */
    private Stores ensureOwnerOfStore(Long storeId) {
        // SecurityContext에서 Authentication 조회
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            // 인증 정보가 없거나 비정상 → 401
            throw new ApiException(ErrorCode.UNAUTHORIZED, "인증이 필요합니다.");
        }

        // 현재 사용자 권한에 OWNER가 포함되는지 검사
        boolean isOwner = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("OWNER"::equals);
        if (!isOwner) {
            // OWNER 권한이 아니면 → 403
            throw new ApiException(ErrorCode.FORBIDDEN, "가게 카테고리 수정은 OWNER만 가능합니다.");
        }

        // 가게 존재 여부 확인(없으면 404)
        Stores s = storesRepository.findById(storeId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "존재하지 않는 가게입니다."));

        // 소유자 이메일과 현재 사용자(email=username) 비교(불일치 시 403)
        String email = auth.getName();
        if (!s.getOwner().getEmail().equals(email)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "본인 가게만 수정할 수 있습니다.");
        }
        return s;
    }

    /**
     * 요청 카테고리 목록 검증
     * - 중복 제거(EnumSet)
     * - 개수 제약: 1~2개
     * - 0개면 400, 2개 초과면 409 규격으로 예외 처리
     *
     * @param categories 요청 카테고리 리스트
     * @return 유효한 카테고리 집합(EnumSet)
     */
    private static EnumSet<StoreCategory> validateSet(List<StoreCategory> categories) {
        // 중복 제거를 위해 EnumSet 사용
        var set = EnumSet.noneOf(StoreCategory.class);
        set.addAll(categories);

        // 규격에 맞춘 예외 코드: 0개면 400, 2개 초과면 409
        if (set.isEmpty() || set.size() > 2) {
            throw new ApiException(
                    set.isEmpty() ? ErrorCode.BAD_REQUEST : ErrorCode.CONFLICT,
                    set.isEmpty() ? "카테고리는 최소 1개 이상이어야 합니다."
                            : "카테고리는 최대 2개까지 설정할 수 있습니다."
            );
        }
        return set;
    }

    /**
     * 등록(처음 1회)
     * - 이미 등록된 카테고리가 있으면 409(CONFLICT)
     * - 유효성 검증 후 링크 저장, 저장 결과를 조회해 응답
     */
    @Transactional
    public StoreCategoriesResponse create(Long storeId, StoreCategoriesRequest req) {
        // 가게의 소유권 검증
        Stores store = ensureOwnerOfStore(storeId);

        // 이미 카테고리가 존재하면 등록 불가 → 409
        long existing = linkRepository.countByStore_Id(storeId);
        if (existing > 0)
            throw new ApiException(ErrorCode.CONFLICT, "이미 등록된 카테고리가 있습니다. 수정 API를 사용하세요.");

        // 요청 카테고리 검증(0개=400, 초과=409)
        var set = validateSet(req.getCategories());

        // 요청한 카테고리들을 링크 엔티티로 저장
        set.forEach(cat -> linkRepository.save(
                StoreCategoryLink.builder().store(store).category(cat).build()
        ));
        // 현재 상태 재조회 후 반환
        var now = linkRepository.findByStore_Id(storeId).stream()
                .map(StoreCategoryLink::getCategory)
                .map(StoreCategory::getLabel)
                .toList();

        return new StoreCategoriesResponse(storeId, now);
    }

    /**
     * 수정(전체 치환) — PUT
     * - 기존 링크 전체 삭제 후, 요청 세트로 재생성
     * - validateSet으로 0개/초과 규격 강제(400/409)
     */
    @Transactional
    public StoreCategoriesResponse update(Long storeId, StoreCategoriesRequest req) {
        // 가게의 소유권 검증
        Stores store = ensureOwnerOfStore(storeId);
        // 요청 카테고리 검증
        var set = validateSet(req.getCategories());

        // 전체 삭제 후 새로 저장 (치환)
        linkRepository.deleteByStore_Id(storeId);
        set.forEach(cat -> linkRepository.save(
                StoreCategoryLink.builder().store(store).category(cat).build()
        ));

        // 현재 상태 조회 후 반환
        var now = linkRepository.findByStore_Id(storeId).stream()
                .map(StoreCategoryLink::getCategory)
                .map(StoreCategory::getLabel)
                .toList();

        return new StoreCategoriesResponse(storeId, now);
    }

    /**
     * 단일 삭제
     * - 특정 카테고리만 제거
     * - 최소 0~2개까지의 제약은 서비스 상에서 create/update 시 강제,
     *   단일 삭제는 그 결과가 0개가 될 수도 있음 (요구사항에 맞게 허용)
     */
    @Transactional
    public StoreCategoriesDeleteResponse removeOne(Long storeId, StoreCategory category) {
        // 오너·소유권 검증
        ensureOwnerOfStore(storeId);

        // 존재 여부 확인(없으면 404)
        boolean exists = linkRepository.existsByStore_IdAndCategory(storeId, category);
        if (!exists) {
            throw new ApiException(ErrorCode.NOT_FOUND, "해당 카테고리가 존재하지 않습니다.");
        }

        // 단일 삭제 수행
        linkRepository.deleteByStore_IdAndCategory(storeId, category);

        // 현재 상태 조회 후 반환 + 안내 메시지
        var now = linkRepository.findByStore_Id(storeId).stream()
                .map(StoreCategoryLink::getCategory).map(StoreCategory::getLabel).toList();
        String msg = "카테고리 '" + category.getLabel() + "'을(를) 삭제했습니다.";

        return new StoreCategoriesDeleteResponse(storeId, now, msg);
    }

    /**
     * 전체 삭제
     * - 등록된 카테고리가 하나도 없으면 404
     * - 성공 시 빈 목록과 메시지 반환
     */
    @Transactional
    public StoreCategoriesDeleteResponse removeAll(Long storeId) {
        // 오너·소유권 검증
        ensureOwnerOfStore(storeId);

        // 하나라도 존재하는지 확인(없으면 404)
        boolean any = linkRepository.existsByStore_Id(storeId);
        if (!any) {
            throw new ApiException(ErrorCode.NOT_FOUND, "삭제할 카테고리가 없습니다.");
        }

        // 전체 삭제
        linkRepository.deleteByStore_Id(storeId);

        // 삭제 후 상태(보통 빈 리스트)와 메시지 반환
        var now = linkRepository.findByStore_Id(storeId).stream()
                .map(StoreCategoryLink::getCategory).map(StoreCategory::getLabel).toList();
        return new StoreCategoriesDeleteResponse(storeId, now, "카테고리를 삭제했습니다.");
    }

    /**
     * 조회(오너만)
     * - 대상 가게가 없으면 404
     * - 오너가 아니면 403 (ensureOwnerOfStore 내부)
     * - 현재 등록된 카테고리 목록 반환
     */
    @Transactional
    public StoreCategoriesResponse get(Long storeId) {
        // 가게 존재 여부 확인
        storesRepository.findById(storeId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "존재하지 않는 가게입니다."));

        // 오너만 조회할 수 있도록 권한 검증
        ensureOwnerOfStore(storeId);  // 오너 검증

        // 가게의 카테고리 목록 조회
        var now = linkRepository.findByStore_Id(storeId).stream()
                .map(StoreCategoryLink::getCategory)
                .map(StoreCategory::getLabel)
                .toList();

        return new StoreCategoriesResponse(storeId, now);
    }
}
