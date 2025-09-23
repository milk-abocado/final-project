package com.example.finalproject.domain.elasticsearchpopular.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Entity
@Table(name = "popular_searches")
@Getter
@Setter
public class PopularSearch {
    @jakarta.persistence.Id
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String keyword;
    private String region;
    private int count;
    private int rank;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
