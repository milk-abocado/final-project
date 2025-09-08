package com.example.finalproject.domain.searches.controller;

import com.example.finalproject.domain.searches.dto.SearchesRequestDto;
import com.example.finalproject.domain.searches.dto.SearchesResponseDto;
import com.example.finalproject.domain.searches.service.SearchesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/searches")
public class SearchesController {
    private final SearchesService searchesService;
    public SearchesController(SearchesService searchesService) {
        this.searchesService = searchesService;
    }

    //검색 기록 등록 기능
    @PostMapping
    public ResponseEntity<SearchesResponseDto> create(@RequestBody SearchesRequestDto request) {
        SearchesResponseDto response = searchesService.saveOrUpdate(request);
        return ResponseEntity.status(201).body(response);
    }

    //검색 기록 업데이트 기능
    @PutMapping
    public ResponseEntity<SearchesResponseDto> update(@RequestBody SearchesRequestDto request) {
        SearchesResponseDto response = searchesService.saveOrUpdate(request);
        return ResponseEntity.status(200).body(response);
    }
}