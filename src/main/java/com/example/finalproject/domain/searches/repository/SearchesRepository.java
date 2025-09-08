package com.example.finalproject.domain.searches.repository;

import com.example.finalproject.domain.searches.entity.Searches;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SearchesRepository extends JpaRepository<Searches,Long> {
    Optional<Searches> findByUserIdAandKeywordAndRegion(Long userId, String keyword, String region);
}
