package com.example.finalproject.domain.users.repository;

import com.example.finalproject.domain.users.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsersRepository extends JpaRepository<Users, Long> {

    // 이메일 존재여부
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);

    // 이메일로 조회 (대소문자 무시)
    Optional<Users> findByEmailIgnoreCase(String email);

    // 만약 Users 엔티티에 `deleted`(boolean) 컬럼이 있다면 ↓ 두 개도 함께 사용
    Optional<Users> findByEmailIgnoreCaseAndDeletedFalse(String email);
    boolean existsByEmailIgnoreCaseAndDeletedFalse(String email);
}