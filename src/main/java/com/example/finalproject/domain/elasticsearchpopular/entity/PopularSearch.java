package com.example.finalproject.domain.elasticsearchpopular.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class PopularSearch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String keyword;
    private String region;
    private int count;
    private int rank;
}

