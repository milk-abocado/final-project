package com.example.finalproject.domain.menus.repository;

import com.example.finalproject.domain.menus.entity.MenuOptionChoices;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface MenuOptionChoicesRepository extends CrudRepository<MenuOptionChoices, Long> {
    List<MenuOptionChoices> findByGroup_Id(Long groupId);
}
