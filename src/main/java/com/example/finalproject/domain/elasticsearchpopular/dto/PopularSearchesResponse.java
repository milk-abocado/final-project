package com.example.finalproject.domain.elasticsearchpopular.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PopularSearchesResponse {
    private Long id;
    private String keyword;
    private String region;
    private Integer ranking;
    private Integer searchCount;
}
