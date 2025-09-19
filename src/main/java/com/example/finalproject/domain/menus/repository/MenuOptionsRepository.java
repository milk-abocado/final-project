package com.example.finalproject.domain.menus.repository;

import com.example.finalproject.domain.menus.entity.MenuOptions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuOptionsRepository extends JpaRepository<MenuOptions, Integer> {
    List<MenuOptions> findByMenuId(Long menuId);
}
