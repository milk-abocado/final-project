package com.example.finalproject.domain.searches.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SearchesResponseDto {
    private Long id;
    private String keyword;
    private String region;
    private Integer count;
    private LocalDateTime updatedAt;
    private Long userId;
}