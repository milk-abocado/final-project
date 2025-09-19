package com.example.finalproject.domain.searches.dto;

import lombok.Data;

@Data
public class SearchesRequestDto {
    private String keyword;
    private String region;
    private Long userId;
}