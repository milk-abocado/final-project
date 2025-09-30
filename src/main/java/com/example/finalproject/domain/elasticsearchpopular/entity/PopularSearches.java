package com.example.finalproject.domain.elasticsearchpopular.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "popular_searches")
@Getter
@Setter
@NoArgsConstructor @AllArgsConstructor
public class PopularSearches {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String keyword;
    private String region;

    @Column(name = "ranking")
    private int ranking;

    @Column(name = "search_count")
    private int searchCount;

    @Column(name =  "created_at")
    private LocalDateTime createdAt;
}

