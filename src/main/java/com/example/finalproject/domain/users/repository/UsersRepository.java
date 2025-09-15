package com.example.finalproject.domain.users.repository;

import com.example.finalproject.domain.users.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsersRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByEmailAndDeletedFalse(String email);
    Optional<Users> findByEmail(String email);
    boolean existsByEmail(String email);
}
