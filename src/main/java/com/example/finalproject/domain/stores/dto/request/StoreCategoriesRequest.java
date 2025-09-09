package com.example.finalproject.domain.stores.dto.request;

import com.example.finalproject.domain.stores.category.StoreCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.UniqueElements;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * StoreCategoriesRequest
 * -------------------------------------------------
 * - 가게 카테고리 등록/수정 요청 DTO
 * - 클라이언트가 가게에 설정하고자 하는 카테고리 목록을 전달
 * - 유효성 검증을 통해 카테고리 개수 및 중복 여부를 제한
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StoreCategoriesRequest {

        /*
          카테고리 목록
          - 최소 1개 이상, 최대 2개까지 허용
          - 같은 카테고리 중복 불가
         */
        @NotNull
        @Size(min = 1, max = 2, message = "카테고리는 1~2개까지 설정할 수 있습니다.")
        @UniqueElements(message = "같은 카테고리를 중복으로 보낼 수 없습니다.")
        private List<StoreCategory> categories;
}
