package com.example.finalproject.domain.stores.entity;

import jakarta.persistence.*;
import lombok.*;
import com.example.finalproject.domain.stores.auth.Role;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users") // DB 테이블 이름 지정
public class Users {

    // 기본 키 (PK), Auto Increment
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 이메일 (로그인 계정으로 사용, 유니크 제약조건)
    @Column(nullable = false, length = 100, unique = true)
    private String email;

    // 비밀번호
    @Column(nullable = false)
    private String password;

    // 사용자 이름
    @Column(nullable = false, length = 50)
    private String name;

    // 권한 (USER, OWNER, ADMIN) - Enum 매핑
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
}
