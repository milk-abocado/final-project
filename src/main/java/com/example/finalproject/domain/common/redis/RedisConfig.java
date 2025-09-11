package com.example.finalproject.domain.common.redis;

import org.springframework.context.annotation.*;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;


@Configuration
public class RedisConfig {
    @Bean public RedisConnectionFactory redisConnectionFactory(){ return new LettuceConnectionFactory(); }
    @Bean public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory cf){ return new StringRedisTemplate(cf); }
}