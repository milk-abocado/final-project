package com.example.finalproject.domain.stores.repository;

import com.example.finalproject.domain.stores.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsersRepository extends JpaRepository<Users, Long> {
    // 현재는 기본 CRUD 메서드만 사용
}
