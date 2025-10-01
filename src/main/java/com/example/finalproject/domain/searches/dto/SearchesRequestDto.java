package com.example.finalproject.domain.searches.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchesRequestDto {
    private String keyword;
    private String region;
    private Long userId;
}