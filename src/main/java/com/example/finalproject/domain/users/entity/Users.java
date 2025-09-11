package com.example.finalproject.domain.users.entity;


import com.example.finalproject.domain.users.UserRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "users")
public class Users {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(unique = true, length = 100, nullable = false)
    private String email;
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    private String nickname;
    private String address;
    private boolean deleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist void onCreate(){ createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate void onUpdate(){ updatedAt = LocalDateTime.now(); }
}
