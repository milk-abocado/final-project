package com.example.finalproject.domain.searches.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "searches", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "keyword", "region"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Searches {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String keyword;

    @Column(nullable = false, length = 50)
    private String region;

    @Column(nullable = false)
    private Integer count = 1;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @PrePersist
    public void onCreate() {
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
