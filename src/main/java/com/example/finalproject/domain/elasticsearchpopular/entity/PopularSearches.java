package com.example.finalproject.domain.elasticsearchpopular.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "popular_search")
@Getter
@Setter
@NoArgsConstructor @AllArgsConstructor
public class PopularSearches {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String keyword;
    private String region;
    private int search_count;
    private int ranking;

    @Column(name =  "created_at")
    private LocalDateTime createdAt;
}

