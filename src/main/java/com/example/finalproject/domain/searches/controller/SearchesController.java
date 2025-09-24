package com.example.finalproject.domain.searches.controller;

import com.example.finalproject.config.CurrentUser;
import com.example.finalproject.domain.searches.dto.SearchesRequestDto;
import com.example.finalproject.domain.searches.dto.SearchesResponseDto;
import com.example.finalproject.domain.searches.service.SearchesService;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/searches")
public class SearchesController {
    private final SearchesService searchesService;

    public SearchesController(SearchesService searchesService) {
        this.searchesService = searchesService;
    }

    //검색 기록 등록 기능
    @PostMapping
    public ResponseEntity<SearchesResponseDto> create(
            @RequestBody SearchesRequestDto request,
            @AuthenticationPrincipal CurrentUser userDetails // 로그인 유저일 경우
    ) throws BadRequestException {
        request.setUserId(userDetails.getId());
        SearchesResponseDto response = searchesService.saveOrUpdate(request);
        return ResponseEntity.status(201).body(response);
    }

    //검색 기록 업데이트 기능
    @PutMapping
    public ResponseEntity<SearchesResponseDto> update(
            @RequestBody SearchesRequestDto request,
           // @RequestHeader Long userId 임시 헤더일 경우
            @AuthenticationPrincipal CurrentUser userDetails
    ) throws BadRequestException {
        request.setUserId(userDetails.getId()); //dto에 userId 주입
        SearchesResponseDto response = searchesService.saveOrUpdate(request);
        return ResponseEntity.status(200).body(response);
    }

    /**
     * 특정 사용자 검색 기록 조회
     */
    @GetMapping("/me")
    public ResponseEntity<List<SearchesResponseDto>> getMySearches(
            @RequestParam(required = false) String region,
            @RequestParam(required = false, defaultValue = "updatedAt") String sort,
          // @RequestHeader Long userId
            @AuthenticationPrincipal CurrentUser userDetails //로그인 인증에서 유저 정보 받기
    ) {
        Long userId = userDetails.getId();

        List<SearchesResponseDto> result = searchesService.getMySearches(userId, region, sort);

        // response에서 userId는 빼고 내려주고 싶다면 map 단계에서 제거 가능
        result.forEach(r -> r.setUserId(null));

        return ResponseEntity.ok(result);
    }

    /**
     * 특정 검색 기록 단건 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<SearchesResponseDto> getSearchById(
            @PathVariable Long id,
          // @RequestHeader Long userId
            @AuthenticationPrincipal CurrentUser userDetails
    ) {
        SearchesResponseDto response = searchesService.getSearchById(userDetails.getId(), id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteSearches(
            @PathVariable Long id,
           // @RequestHeader Long userId
            @AuthenticationPrincipal CurrentUser userDetails
    ) {
        searchesService.deleteSearches(userDetails.getId(), id);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "검색 기록이 삭제되었습니다.");
        response.put("id", id);

        return ResponseEntity.ok(response);
    }
}
