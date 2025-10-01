package com.example.finalproject.domain.elasticsearchpopular.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * PopularSearches 엔티티
 * 인기 검색어 데이터를 저장하는 테이블 매핑 클래스
 * - 지역별 검색 키워드와 그 검색량, 순위 등을 관리
 */
@Entity
@Table(
        name = "popular_searches",
        uniqueConstraints = @UniqueConstraint(columnNames = {"region", "keyword"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PopularSearches {

    /** 고유 식별자 (PK) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 검색 키워드 */
    private String keyword;

    /** 검색 지역 */
    private String region;

    /** 인기 검색어 순위 (1위, 2위, ...) */
    @Column(name = "ranking")
    private int ranking;

    /** 검색 횟수 */
    @Column(name = "search_count")
    private int searchCount;

    /** 레코드 생성 시각 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** 레코드 수정 시각 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
