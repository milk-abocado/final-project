package com.example.finalproject.domain.stores.dto.response;

import com.example.finalproject.domain.stores.category.StoreCategory;
import com.example.finalproject.domain.stores.entity.StoreCategoryLink;
import com.example.finalproject.domain.stores.entity.Stores;
import lombok.Getter;

import java.util.Comparator;
import java.util.List;

/**
 * StoreCategoriesResponse
 * -------------------------------------------------
 * - 특정 가게의 카테고리 목록을 반환하는 DTO
 * - 가게 ID와 해당 가게에 등록된 카테고리(한글 라벨 포함) 리스트를 포함
 */
@Getter
public class StoreCategoriesResponse {

    private Long storeId;            // 가게 ID
    private List<String> categories; // 카테고리 한글 라벨 목록

    public StoreCategoriesResponse(Long storeId, List<String> categories) {
        this.storeId = storeId;
        this.categories = categories;
    }

    /**
     * Stores 엔티티에서 DTO 변환 메서드
     *
     * @param s Stores 엔티티
     * @return StoreCategoriesResponse DTO
     * - StoreCategoryLink 엔티티에서 카테고리 enum 추출
     * - Enum 이름 기준 정렬 후, 한글 라벨(getLabel) 반환
     */
    public static StoreCategoriesResponse of(Stores s) {
        var cats = s.getCategoryLinks().stream()
                .map(StoreCategoryLink::getCategory)
                .sorted(Comparator.comparing(Enum::name))
                .map(StoreCategory::getLabel) // 한글 라벨 변환
                .toList();

        return new StoreCategoriesResponse(s.getId(), cats);
    }
}
