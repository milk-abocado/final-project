package com.example.finalproject.domain.users.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "users")
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String name;
    private String nickname;
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private Role role; // USER, OWNER, ADMIN

    private Boolean socialLogin = false;
    private Boolean allowNotifications = false;
    private Boolean isDeleted = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum Role {
        USER, OWNER, ADMIN
    }
}
