package com.example.finalproject.domain.points.repository;

import com.example.finalproject.domain.points.entity.Points;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointsRepository extends JpaRepository<Points, Long> {

    List<Points> findByUserId(Long userId);
}
