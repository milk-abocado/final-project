package com.example.finalproject.domain.searches.service;

import com.example.finalproject.domain.searches.dto.SearchesRequestDto;
import com.example.finalproject.domain.searches.dto.SearchesResponseDto;
import com.example.finalproject.domain.searches.entity.Searches;
import com.example.finalproject.domain.searches.exception.SearchesErrorCode;
import com.example.finalproject.domain.searches.exception.SearchesException;
import com.example.finalproject.domain.searches.repository.SearchesRepository;
import com.example.finalproject.domain.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private final StringRedisTemplate redisTemplate;

    public SearchesResponseDto saveOrUpdate(SearchesRequestDto request) {
        // ✅ 400 검증
        if (request.getKeyword() == null || request.getRegion() == null) {
            throw new SearchesException(SearchesErrorCode.BAD_REQUEST, "keyword/region 누락");
        }
        if (request.getKeyword().length() > 100 || request.getRegion().length() > 50) {
            throw new SearchesException(SearchesErrorCode.BAD_REQUEST, "길이 초과");
        }

        // ✅ 401 검증
        if (request.getUserId() == null) {
            throw new SearchesException(SearchesErrorCode.UNAUTHORIZED, "로그인 필요");
        }

        // ✅ 404 검증
        if (!usersRepository.existsById(request.getUserId())) {
            throw new SearchesException(SearchesErrorCode.NOT_FOUND, "존재하지 않는 userId");
        }

        // 검색 기록 찾기
        Searches searches = searchesRepository.findByUserIdAndKeywordAndRegion(
                request.getUserId(), request.getKeyword(), request.getRegion()).orElse(null);

        if (searches != null) {
            searches.setCount(searches.getCount() + 1);
        } else {
            searches = Searches.builder()
                    .keyword(request.getKeyword())
                    .region(request.getRegion())
                    .userId(request.getUserId())
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

    @Transactional(readOnly = true)
    public List<SearchesResponseDto> getMySearches(Long userId, String region, String sort) {
        if (userId == null) {
            throw new SearchesException(SearchesErrorCode.UNAUTHORIZED, "로그인 필요");
        }

        List<Searches> searches = (region != null && !region.isEmpty())
                ? searchesRepository.findByUserIdAndRegion(userId, region)
                : searchesRepository.findByUserId(userId);

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

    // 인기 검색어 기록
    public void recordSearch(String keyword, String region, Long userId) {
        try {
            SearchesRequestDto dto = new SearchesRequestDto(keyword, region, userId);
            saveOrUpdate(dto); // DB 기록 재사용

            // Redis 증가
            String redisKey = "popular:" + region + ":" + keyword;
            redisTemplate.opsForValue().increment(redisKey, 1);

        } catch (SearchesException e) {
            throw e;
        } catch (Exception e) {
            throw new SearchesException(SearchesErrorCode.INTERNAL_ERROR, "검색 기록 저장 실패: " + e.getMessage());
        }
    }
}
