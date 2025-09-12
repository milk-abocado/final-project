package com.example.finalproject.domain.menus.repository;

import com.example.finalproject.domain.menus.entity.MenuCategories;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuCategoriesRepository extends JpaRepository<MenuCategories, Integer> {
    List<MenuCategories> findByMenuId(Long menuId);
}
