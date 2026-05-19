package com.xjtu.iron.cache.provider.redis;


import java.time.Duration;

public interface RedisBinaryClient {

    byte[] get(String key);

    void set(String key, byte[] value, Duration ttl);

    void del(String key);

    default void put(String key, byte[] value, Duration ttl) {
        set(key, value, ttl);
    }
}
