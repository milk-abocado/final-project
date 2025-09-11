package com.example.finalproject.domain.users.repository;

import com.example.finalproject.domain.users.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsersRepository extends JpaRepository<Users, Long> {
}
