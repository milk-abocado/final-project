package com.example.finalproject.domain.stores.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * UserStar (즐겨찾기 엔티티)
 *
 * - 사용자와 가게 간의 즐겨찾기 관계를 나타내는 엔티티
 * - 한 사용자가 같은 가게를 여러 번 즐겨찾기하지 못하도록 Unique 제약 조건 적용
 */
@Entity
@Table(name = "user_stars",
        // 사용자-가게 조합의 유니크 제약 조건
        uniqueConstraints = @UniqueConstraint(name = "uk_user_store", columnNames = {"user_id","store_id"}),
        indexes = {
                // 사용자별 즐겨찾기 등록 시간 인덱스
                @Index(name = "idx_user_created_at", columnList = "user_id, created_at"),
        })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class UserStar {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 즐겨찾기한 사용자
     * - Users 엔티티와 다대일(N:1) 관계
     * - 반드시 값이 존재해야 하므로 optional=false
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    /**
     * 즐겨찾기한 가게
     * - Stores 엔티티와 다대일(N:1) 관계
     * - 반드시 값이 존재해야 하므로 optional=false
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Stores store;

    /**
     * 즐겨찾기 등록 시각
     * - 엔티티가 처음 생성될 때 자동으로 시간 기록
     * - 이후 업데이트 불가 (updatable=false)
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
