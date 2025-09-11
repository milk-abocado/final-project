package com.example.finalproject.domain.auth.entity;

import com.example.finalproject.domain.users.entity.Users;
import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "social_accounts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "providerId"}))
public class SocialAccount {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Users user;

    @Column(nullable = false) private String provider;   // kakao, naver
    @Column(nullable = false) private String providerId; // IdP의 고유 id
}