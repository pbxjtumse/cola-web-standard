package com.xjtu.iron.cache.provider.redis;

import java.time.Duration;

/**
 * Redis 二进制客户端适配接口。
 *
 * <p>这一层表达 Redis 命令语义，使用 get/set/del 命名；
 * 缓存语义由 RedisCacheProvider 负责。</p>
 */
public interface RedisBinaryClient {

    /** 读取 Redis byte[] value。 */
    byte[] get(String key);

    /** 写入 Redis byte[] value，并设置 TTL。 */
    void set(String key, byte[] value, Duration ttl);

    /** 删除 Redis key。 */
    void del(String key);

    /** set 的语义别名。 */
    default void put(String key, byte[] value, Duration ttl) {
        set(key, value, ttl);
    }
}
