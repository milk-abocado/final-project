package com.example.finalproject.domain.searches.service;

import com.example.finalproject.domain.searches.dto.SearchesRequestDto;
import com.example.finalproject.domain.searches.dto.SearchesResponseDto;
import com.example.finalproject.domain.searches.entity.Searches;
import com.example.finalproject.domain.searches.repository.SearchesRepository;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class SearchesService {
    private final SearchesRepository searchesRepository;

    public SearchesService(SearchesRepository searchesRepository) {
        this.searchesRepository = searchesRepository;
    }

    public SearchesResponseDto saveOrUpdate(SearchesRequestDto request) {
        //400: keyword/region 누락, 길이 초과
        if (request.getKeyword() == null || request.getRegion() == null) {
            throw new BadRequestException("keyword/region 누락");
        }
        if (request.getKeyword().length() > 100 || request.getRegion().length() > 50) {
            throw new BadRequestException("길이 초과");
        }

        //401: 인증 실패(로그인 필요)
        if (request.getUserId() == null) {
            throw new UnauthorizedException("로그인 필요");
        }

        //404: 존재하지 않는 user_id 참조
        if (!userRepository.existsById(request.getUserId())) {
            throw new NotFoundException("존재하지 않는 userId");
        }

        //검색 기록 찾기
        Searches searches = searchesRepository.findByUserIdAndKeywordAndRegion(
                request.getUserId(), request.getKeyword(), request.getRegion()).orElse(null);

        if (searches != null) {
            //동일 조합 있으면 count +1
            searches.setCount(searches.getCount() + 1);
        }  else {
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
    }
