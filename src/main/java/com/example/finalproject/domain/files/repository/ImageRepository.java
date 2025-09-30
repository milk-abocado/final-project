package com.example.finalproject.domain.files.repository;

import com.example.finalproject.domain.files.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findByRefTypeAndRefId(String refType, Long refId);
}