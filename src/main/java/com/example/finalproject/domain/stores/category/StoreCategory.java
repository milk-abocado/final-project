package com.example.finalproject.domain.stores.category;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum StoreCategory {
    KOREAN("한식"),
    CHINESE("중식"),
    JAPANESE("일식"),
    WESTERN("양식"),
    SNACK("분식"),
    CHICKEN("치킨"),
    PIZZA("피자"),
    BURGER("햄버거"),
    MEAT("고기"),
    SANDWICH_SALAD("샌드위치/샐러드"),
    DESSERT_CAFE("디저트/카페"),
    LATE_NIGHT("야식");

    private final String label;

    StoreCategory(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    /** RequestBody(JSON)에서 한글/영문 모두 허용 */
    @JsonCreator
    public static StoreCategory from(String value) {
        if (value == null) return null;
        for (StoreCategory c : StoreCategory.values()) {
            if (c.label.equals(value)) return c;                // 한글 매칭
            if (c.name().equalsIgnoreCase(value)) return c;     // 영문 코드 매칭
        }
        throw new IllegalArgumentException("잘못된 카테고리: " + value);
    }

    /** PathVariable 등에서 직접 변환할 때 사용할 유틸 */
    public static StoreCategory fromPath(String value) {
        return from(value);
    }
}
