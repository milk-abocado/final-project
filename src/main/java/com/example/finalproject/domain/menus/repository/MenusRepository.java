package com.example.finalproject.domain.menus.repository;

import com.example.finalproject.domain.menus.entity.Menus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MenusRepository extends JpaRepository<Menus, Long> {
    List<Menus> findByStoreId(Long storeId);
    Optional<Menus> findByIdAndStoreId(Long id, Long storeId);
    boolean existsByStoreIdAndName(Long storeId, String name);
    Optional<Menus> findByStoreIdAndNameAndStatus(Long storeId, String name, Menus.MenuStatus status);
}
