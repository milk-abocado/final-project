package com.example.finalproject.domain.elasticsearchpopular.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PopularSearchDoc {
    private String keyword;
    private String region;
    private int search_count;
    private LocalDateTime createdAt;
}
