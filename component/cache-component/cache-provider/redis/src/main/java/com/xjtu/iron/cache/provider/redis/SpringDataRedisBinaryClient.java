package com.xjtu.iron.cache.provider.redis;


import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

public class SpringDataRedisBinaryClient implements RedisBinaryClient {

    private final RedisTemplate<String, byte[]> redisTemplate;

    public SpringDataRedisBinaryClient(RedisTemplate<String, byte[]> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public byte[] get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void set(String key, byte[] value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    @Override
    public void del(String key) {
        redisTemplate.delete(key);
    }
}
