package com.example.finalproject.domain.elasticsearchpopular.repository;

import com.example.finalproject.domain.elasticsearchpopular.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegionRepository extends JpaRepository<Region,Long> {
    List<Region> findAll();
}
