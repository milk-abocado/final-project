package com.example.finalproject.domain.stores.entity;

import com.example.finalproject.domain.stores.category.StoreCategory;
import jakarta.persistence.*;
import lombok.*;

/**
 * StoreCategoryLink
 * -------------------------------------------------
 * - 가게와 카테고리(StoreCategory enum) 간의 매핑을 담당하는 엔티티
 * - 하나의 가게(Stores)가 여러 카테고리와 연결될 수 있음 (N:1 관계)
 * - 같은 가게에 동일한 카테고리를 중복 등록할 수 없도록 Unique 제약 조건 적용
 */
@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "store_categories",
        uniqueConstraints = @UniqueConstraint(name = "uk_store_category", columnNames = {"store_id","category"}))
public class StoreCategoryLink {

    // 기본 키(PK), Auto Increment
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 가게 엔티티 (N:1)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Stores store;

    // 카테고리 (Enum -> String으로 저장)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StoreCategory category;
}
