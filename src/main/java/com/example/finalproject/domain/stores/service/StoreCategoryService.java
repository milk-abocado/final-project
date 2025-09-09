package com.example.finalproject.domain.stores.service;

import com.example.finalproject.domain.stores.auth.Role;
import com.example.finalproject.domain.stores.auth.SecurityUtil;
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
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;

/**
 * StoreCategoryService
 * -------------------------------------------------
 * - 가게(Store)에 연결된 카테고리(StoreCategory) 관리 서비스
 * - 등록(최초 1회), 전체 수정(치환), 단일 삭제, 조회 기능 제공
 * - OWNER 본인 가게에 대해서만 등록/수정/삭제 가능 (인가 포함)
 */
@Service
@RequiredArgsConstructor
public class StoreCategoryService {

    private final StoresRepository storesRepository;
    private final StoreCategoryLinkRepository linkRepository;
    private final SecurityUtil security;

    /**
     * 인가 & 소유권 검증
     * - 현재 로그인 사용자가 OWNER인지 확인
     * - 대상 가게 존재 여부 확인
     * - 가게 소유자와 현재 사용자 일치 여부 확인
     *
     * @param storeId 대상 가게 ID
     * @return Stores 엔티티 (검증 통과 시)
     */
    private Stores ensureOwnerOfStore(Long storeId) {
        if (security.currentRole() != Role.OWNER)
            throw new ApiException(ErrorCode.FORBIDDEN, "가게 카테고리 수정은 OWNER만 가능합니다.");
        Stores s = storesRepository.findById(storeId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "존재하지 않는 가게입니다."));
        if (!s.getOwner().getId().equals(security.currentUserId()))
            throw new ApiException(ErrorCode.FORBIDDEN, "본인 가게만 수정할 수 있습니다.");
        return s;
    }

    /**
     * 요청 카테고리 목록 검증
     * - 중복 제거를 위해 EnumSet 사용
     * - 개수 제약: 1~2개
     *
     * @param categories 요청 카테고리 리스트
     * @return 유효한 카테고리 집합(EnumSet)
     */
    private static EnumSet<StoreCategory> validateSet(List<StoreCategory> categories) {
        var set = EnumSet.noneOf(StoreCategory.class);
        set.addAll(categories);
        if (set.isEmpty() || set.size() > 2)
            throw new ApiException(ErrorCode.BAD_REQUEST, "카테고리는 1~2개까지 설정할 수 있습니다.");
        return set;
    }

    /**
     * 등록(처음 1회)
     * - 이미 등록된 카테고리가 1개 이상 존재하면 409(CONFLICT)
     * - 저장 후 현재 상태를 조회하여 응답
     */
    @Transactional
    public StoreCategoriesResponse create(Long storeId, StoreCategoriesRequest req) {
        Stores store = ensureOwnerOfStore(storeId);

        long existing = linkRepository.countByStore_Id(storeId);
        if (existing > 0)
            throw new ApiException(ErrorCode.CONFLICT, "이미 등록된 카테고리가 있습니다. 수정 API를 사용하세요.");

        var set = validateSet(req.categories());
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
     * - 기존 모든 링크 삭제 후, 요청 세트로 재생성
     */
    @Transactional
    public StoreCategoriesResponse update(Long storeId, StoreCategoriesRequest req) {
        Stores store = ensureOwnerOfStore(storeId);
        var set = validateSet(req.categories());

        // 전체 삭제 후 새로 저장 (치환)
        linkRepository.deleteByStore_Id(storeId);
        set.forEach(cat -> linkRepository.save(
                StoreCategoryLink.builder().store(store).category(cat).build()
        ));
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
        ensureOwnerOfStore(storeId);
        linkRepository.deleteByStore_IdAndCategory(storeId, category);

        var now = linkRepository.findByStore_Id(storeId).stream()
                .map(StoreCategoryLink::getCategory)
                .map(StoreCategory::getLabel)
                .toList();

        // 카테고리명을 포함한 메시지 (원하면 일반 메시지로 바꿔도 됨)
        String msg = "카테고리 '" + category.getLabel() + "'을(를) 삭제했습니다"; // ← 여기!
        return new StoreCategoriesDeleteResponse(storeId, now, msg);
    }

    /**
     * 전체 삭제
     * - 가게의 전체 카테고리 제거
     */
    @Transactional
    public StoreCategoriesDeleteResponse removeAll(Long storeId) {
        ensureOwnerOfStore(storeId);
        linkRepository.deleteByStore_Id(storeId);

        // 현재 상태 조회 (삭제 후니까 보통 빈 리스트)
        var now = linkRepository.findByStore_Id(storeId).stream()
                .map(StoreCategoryLink::getCategory)
                .map(StoreCategory::getLabel)
                .toList();

        return new StoreCategoriesDeleteResponse(storeId, now, "카테고리를 삭제했습니다.");
    }

    /**
     * 조회(누구나)
     * - 가게 존재 여부만 확인하고 카테고리 목록 반환
     */
    @Transactional
    public StoreCategoriesResponse get(Long storeId) {
        storesRepository.findById(storeId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "존재하지 않는 가게입니다."));

        var now = linkRepository.findByStore_Id(storeId).stream()
                .map(StoreCategoryLink::getCategory)
                .map(StoreCategory::getLabel)
                .toList();

        return new StoreCategoriesResponse(storeId, now);
    }
}
