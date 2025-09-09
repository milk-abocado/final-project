package com.example.finalproject.domain.menus.repository;

import com.example.finalproject.domain.menus.entity.Menus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenusRepository extends JpaRepository<Menus, Long> {
}
