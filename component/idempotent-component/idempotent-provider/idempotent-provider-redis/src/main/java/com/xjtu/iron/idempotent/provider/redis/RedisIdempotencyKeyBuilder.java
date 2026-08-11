package com.xjtu.iron.idempotent.provider.redis;

/**
 * Redis Cluster 下用 hash-tag 保证同一幂等记录的操作落在同一 slot。
 */
public final class RedisIdempotencyKeyBuilder {
    private final String prefix;

    public RedisIdempotencyKeyBuilder(String prefix) {
        if (prefix == null || prefix.isBlank()) throw new IllegalArgumentException("keyPrefix must not be blank");
        this.prefix = prefix;
    }

    public String build(String namespace, String key) {
        return prefix + ":{" + namespace + ":" + key + "}";
    }
}
