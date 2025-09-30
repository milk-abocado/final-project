package com.example.finalproject.domain.searches.service;

import com.example.finalproject.domain.searches.dto.SearchesRequestDto;
import com.example.finalproject.domain.searches.dto.SearchesResponseDto;
import com.example.finalproject.domain.searches.entity.Searches;
import com.example.finalproject.domain.searches.exception.SearchesErrorCode;
import com.example.finalproject.domain.searches.exception.SearchesException;
import com.example.finalproject.domain.searches.repository.SearchesRepository;
import com.example.finalproject.domain.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class SearchesService {

    private final SearchesRepository searchesRepository;
    private final UsersRepository usersRepository;

    /**
     * 검색 기록 저장 / 업데이트
     */
    public SearchesResponseDto saveOrUpdate(SearchesRequestDto request) {
        // 400: keyword/region 누락, 길이 초과
        if (request.getKeyword() == null || request.getRegion() == null) {
            throw new SearchesException(SearchesErrorCode.BAD_REQUEST, "keyword/region 누락");
        }
        if (request.getKeyword().length() > 100 || request.getRegion().length() > 50) {
            throw new SearchesException(SearchesErrorCode.BAD_REQUEST, "길이 초과");
        }

        // 401: 인증 실패
        if (request.getUserId() == null) {
            throw new SearchesException(SearchesErrorCode.UNAUTHORIZED, "로그인 필요");
        }

        // 404: 존재하지 않는 userId
        if (!usersRepository.existsById(request.getUserId())) {
            throw new SearchesException(SearchesErrorCode.NOT_FOUND, "존재하지 않는 userId");
        }

        Long userId = request.getUserId();
        String keyword = request.getKeyword();
        String region = request.getRegion();

        // 기존 검색 기록 조회
        List<Searches> searchesList = searchesRepository.findAllByUserIdAndKeywordAndRegion(userId, keyword, region);
        Searches searches;
        if (!searchesList.isEmpty()) {
            searches = searchesList.get(0); // 첫 번째 엔티티만 사용
            searches.setCount(searches.getCount() + 1);
        } else {
            searches = Searches.builder()
                    .userId(userId)
                    .keyword(keyword)
                    .region(region)
                    .count(1)
                    .build();
        }

        Searches saved = searchesRepository.save(searches);

        return SearchesResponseDto.builder()
                .id(saved.getId())
                .keyword(saved.getKeyword())
                .region(saved.getRegion())
                .userId(saved.getUserId())
                .updatedAt(saved.getUpdatedAt())
                .count(saved.getCount())
                .build();
        }

    /**
     * 특정 사용자 검색 기록 조회
     */
    @Transactional(readOnly = true)
    public List<SearchesResponseDto> getMySearches(Long userId, String region, String sort) {
        if (userId == null) {
            throw new SearchesException(SearchesErrorCode.UNAUTHORIZED, "로그인 필요");
        }

        List<Searches> searches;
        if (region != null && !region.isEmpty()) {
            searches = searchesRepository.findByUserIdAndRegion(userId, region);
        } else {
            searches = searchesRepository.findByUserId(userId);
        }

        Comparator<Searches> comparator;
        if (sort == null || sort.isBlank()) {
            comparator = Comparator.comparing(Searches::getUpdatedAt).reversed();
        } else if ("count".equalsIgnoreCase(sort)) {
            comparator = Comparator.comparing(Searches::getCount).reversed();
        } else if ("updatedAt".equalsIgnoreCase(sort)) {
            comparator = Comparator.comparing(Searches::getUpdatedAt).reversed();
        } else {
            throw new SearchesException(SearchesErrorCode.BAD_REQUEST, "잘못된 sort 값");
        }

        return searches.stream()
                .sorted(comparator)
                .map(s -> SearchesResponseDto.builder()
                        .id(s.getId())
                        .keyword(s.getKeyword())
                        .region(s.getRegion())
                        .count(s.getCount())
                        .updatedAt(s.getUpdatedAt())
                        .userId(s.getUserId())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 특정 검색 기록 단건 조회
     */
    @Transactional(readOnly = true)
    public SearchesResponseDto getSearchById(Long userId, Long id) {
        if (userId == null) {
            throw new SearchesException(SearchesErrorCode.UNAUTHORIZED, "로그인 필요");
        }

        Searches searches = searchesRepository.findById(id)
                .orElseThrow(() -> new SearchesException(SearchesErrorCode.NOT_FOUND, "검색 기록 없음"));

        if (!searches.getUserId().equals(userId)) {
            throw new SearchesException(SearchesErrorCode.FORBIDDEN, "다른 사용자의 기록 조회 불가");
        }

        return SearchesResponseDto.builder()
                .id(searches.getId())
                .keyword(searches.getKeyword())
                .region(searches.getRegion())
                .count(searches.getCount())
                .updatedAt(searches.getUpdatedAt())
                .userId(searches.getUserId())
                .build();
    }

    /**
     * 검색 기록 삭제
     */
    @Transactional
    public void deleteSearches(Long userId, Long id) {
        if (userId == null) {
            throw new SearchesException(SearchesErrorCode.UNAUTHORIZED, "로그인 필요");
        }

        Searches searches = searchesRepository.findById(id)
                .orElseThrow(() -> new SearchesException(SearchesErrorCode.NOT_FOUND, "검색 기록 없음"));

        if (!searches.getUserId().equals(userId)) {
            throw new SearchesException(SearchesErrorCode.FORBIDDEN, "다른 사용자 기록 삭제 불가");
        }

        searchesRepository.delete(searches);
    }

    public void recordSearch(String region, String keyword, Long userId) {
    }
}
