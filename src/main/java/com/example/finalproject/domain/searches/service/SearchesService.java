package com.example.finalproject.domain.searches.service;

import com.example.finalproject.domain.searches.dto.SearchesRequestDto;
import com.example.finalproject.domain.searches.dto.SearchesResponseDto;
import com.example.finalproject.domain.searches.entity.Searches;
import com.example.finalproject.domain.searches.exception.SearchesException;
import com.example.finalproject.domain.searches.repository.SearchesRepository;
import com.example.finalproject.domain.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class SearchesService {
    private final SearchesRepository searchesRepository;
    private final UsersRepository usersRepository;

    public SearchesResponseDto saveOrUpdate(SearchesRequestDto request) throws BadRequestException {
        //400: keyword/region 누락, 길이 초과
        if (request.getKeyword() == null || request.getRegion() == null) {
            throw SearchesException.badRequest("keyword/region 누락");
        }
        if (request.getKeyword().length() > 100 || request.getRegion().length() > 50) {
            throw SearchesException.badRequest("길이 초과");
        }

        //401: 인증 실패(로그인 필요)
        if (request.getUserId() == null) {
            throw SearchesException.unauthorized("로그인 필요");
        }

        //404: 존재하지 않는 user_id 참조
        if (!usersRepository.existsById(request.getUserId())) {
            throw SearchesException.notFound("존재하지 않는 userId");
        }

        //검색 기록 찾기
        Searches searches = searchesRepository.findByUserIdAndKeywordAndRegion(
                request.getUserId(), request.getKeyword(), request.getRegion()).orElse(null);

        if (searches != null) {
            //동일 조합 있으면 count +1
            searches.setCount(searches.getCount() + 1);
        } else {
            //없으면 새로 생성
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

    /**
     * 특정 사용자 검색 기록 조회
     */
    @Transactional(readOnly = true)
    public List<SearchesResponseDto> getMySearches(Long userId, String region, String sort) {
        //401 검증 (로그인 안 된 경우)
        if (userId == null) {
            throw SearchesException.unauthorized("로그인 필요");
        }

        List<Searches> searches;
        if (region != null && !region.isEmpty()) {
            searches = searchesRepository.findByUserIdAndRegion(userId, region);
        } else {
            searches = searchesRepository.findByUserId(userId);
        }

        //400 검증: sort 값 유효성
        Comparator<Searches> comparator;
        if (sort == null || sort.isBlank()) {
            comparator = Comparator.comparing(Searches::getUpdatedAt).reversed();
        } else if ("count".equalsIgnoreCase(sort)) {
            comparator = Comparator.comparing(Searches::getCount).reversed();
        } else if ("updatedAt".equalsIgnoreCase(sort)) {
            comparator = Comparator.comparing(Searches::getUpdatedAt).reversed();
        } else {
            throw SearchesException.badRequest("잘못된 sort 값");
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
            throw SearchesException.unauthorized("로그인 필요");
        }

        Searches searches = searchesRepository.findById(id)
                .orElseThrow(() -> SearchesException.notFound("검색 기록 없음"));

        if (!searches.getUserId().equals(userId)) {
            throw SearchesException.forbidden("다른 사용자의 기록 조회 불가");
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
            throw SearchesException.unauthorized("로그인 필요");
        }

        Searches searches = searchesRepository.findById(id)
                .orElseThrow(() -> SearchesException.notFound("검색 기록 없음"));

        if (!searches.getUserId().equals(userId)) {
            throw SearchesException.forbidden("다른 사용자 기록 삭제 불가");
        }

        searchesRepository.delete(searches);
    }

    //인기 검색어: 사용자 검색 처리(Redis 카운트 증가)
    private final StringRedisTemplate redisTemplate;

    // 검색 키워드와 지역별 count 증가
    public void recordSearch(String keyword, String region) {
        // DB 저장
        Searches search = new Searches();
        search.setRegion(region);
        search.setKeyword(keyword);
        searchesRepository.save(search);

        //Redis 증가
        String redisKey = "search_count:" + region + ":" + keyword;
        redisTemplate.opsForValue().increment(redisKey, 1);
    }
}