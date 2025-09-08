package com.example.finalproject.domain.searches.repository;

import com.example.finalproject.domain.searches.entity.Searches;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SearchesRepository extends JpaRepository<Searches,Long> {
    Optional<Searches> findByUserIdAndKeywordAndRegion(Long userId, String keyword, String region);

    List<Searches> findByUserId(Long userId);

    List<Searches> findByUserIdAndRegion(Long userId, String region);
}