package com.example.finalproject.domain.users.repository;

import com.example.finalproject.domain.users.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;


public interface UserRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByEmailAndDeletedFalse(String email);
    Optional<Users> findByIdAndDeletedFalse(Long id);
    boolean existsByEmail(String email);
}