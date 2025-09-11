package com.example.finalproject.domain.points.entity;

import com.example.finalproject.domain.users.Users;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import java.time.LocalDateTime;

@Entity
@Table(name = "points")
@Getter
@NoArgsConstructor
public class Points {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    private int amount;
    private String reason;
    private LocalDateTime createdAt = LocalDateTime.now();

    public Points(Users user, int amount, String reason) {
        this.user = user;
        this.amount = amount;
        this.reason = reason;
    }
}
