package com.example.finalproject.domain.users.entity;

import com.example.finalproject.domain.users.UserRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(
        name = "users",
        indexes = { @Index(name = "ux_users_email", columnList = "email", unique = true) }
)
public class Users {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(length = 30)
    private String nickname;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UserRole role;

    @Column(name = "social_login", nullable = false)
    private boolean socialLogin;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
        if (role == null) role = UserRole.USER;   // 기본 USER
        // 가입은 일반 가입이므로 false
        socialLogin = (socialLogin);              // 명시적으로 유지(기본 false)
    }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }
}