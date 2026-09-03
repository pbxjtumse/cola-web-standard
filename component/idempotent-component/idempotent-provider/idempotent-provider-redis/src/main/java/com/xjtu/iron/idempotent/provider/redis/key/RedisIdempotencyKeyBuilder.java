package com.xjtu.iron.idempotent.provider.redis.key;

/**
 * Redis 幂等记录物理 Key 构造器。
 *
 * <p>V2 使用 {@code storeName + namespace + key} 作为逻辑存储身份，并继续使用 Redis Cluster hash-tag：
 * {@code prefix:{storeName:namespace:key}}。storeName 不是 jdbc/redis Provider 名，而是 message-consume/payment 这类逻辑 Store。</p>
 */
public final class RedisIdempotencyKeyBuilder {

    private final String prefix;

    public RedisIdempotencyKeyBuilder(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("keyPrefix must not be blank");
        }
        this.prefix = prefix;
    }

    /** @return Redis 中实际使用的物理 Key。 */
    public String build(String storeName, String namespace, String key) {
        return prefix + ":{" + storeName + ":" + namespace + ":" + key + "}";
    }
}
