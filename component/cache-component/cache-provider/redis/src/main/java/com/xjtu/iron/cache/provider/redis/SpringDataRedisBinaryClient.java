package com.xjtu.iron.cache.provider.redis;

import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

/**
 * 基于 Spring Data Redis 的 RedisBinaryClient 实现。
 */
public class SpringDataRedisBinaryClient implements RedisBinaryClient {

    /** 专用于缓存组件的 RedisTemplate，value 使用 byte[]。 */
    private final RedisTemplate<String, byte[]> redisTemplate;

    /** 创建 Redis 客户端适配器。 */
    public SpringDataRedisBinaryClient(RedisTemplate<String, byte[]> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** 读取 Redis value。 */
    @Override
    public byte[] get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /** 写入 Redis value。 */
    @Override
    public void set(String key, byte[] value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    /** 删除 Redis key。 */
    @Override
    public void del(String key) {
        redisTemplate.delete(key);
    }
}
