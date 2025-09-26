package com.example.finalproject.domain.carts.repository;

import com.example.finalproject.domain.carts.dto.response.CartsResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

// sql 없어서 JpaRepository 사용 불가
@Repository
public class CartsRepository {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public CartsRepository(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    private String cartKey(Long userId) {
        return "carts:" + userId;
    }

    public CartsResponse getCart(Long userId){
        String json = redisTemplate.opsForValue().get(cartKey(userId));
        if(json == null){
            return null;
        }
        try {
            return objectMapper.readValue(json, CartsResponse.class);
        }
        catch (JsonProcessingException e){
            throw new RuntimeException("Cart parsing error", e);
        }
    }

    public void saveCart(Long userId, CartsResponse cart){
        try{
            redisTemplate.opsForValue().set(cartKey(userId), objectMapper.writeValueAsString(cart));
        }
        catch (JsonProcessingException e){
            throw new RuntimeException("Cart serialization error", e); // 바이트 스트림 형태의 연속적인 데이터로 변환에서 error
        }
    }

    public void deleteCart(Long userId){
        redisTemplate.delete(cartKey(userId));
    }

}
