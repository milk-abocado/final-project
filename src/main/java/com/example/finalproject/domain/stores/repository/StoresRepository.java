package com.example.finalproject.domain.stores.repository;

import com.example.finalproject.domain.stores.entity.Stores;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoresRepository extends JpaRepository<Stores, Long> {
}
