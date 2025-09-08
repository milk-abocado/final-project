package com.example.finalproject.domain.searches.controller;

import com.example.finalproject.domain.searches.dto.SearchesRequest;
import com.example.finalproject.domain.searches.dto.SearchesResponse;
import com.example.finalproject.domain.searches.entity.Searches;
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
    public ResponseEntity<SearchesResponse> create(@RequestBody SearchesRequest request) {
        SearchesResponse response = searchesService.saveOrUpdate(request);
        return ResponseEntity.status(201).body(response);
    }

    //검색 기록 업데이트 기능
    @PutMapping
    public ResponseEntity<SearchesResponse> update(@RequestBody SearchesRequest request) {
        SearchesResponse response = searchesService.saveOrUpdate(request);
        return ResponseEntity.status(200).body(response);
    }
}