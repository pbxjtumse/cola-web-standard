package com.xjtu.iron.idempotent.provider.redis;

/**
 * Redis 幂等记录物理 Key 构造器。
 *
 * <p>使用 Redis Cluster hash-tag：{@code prefix:{namespace:key}}。
 * 这样同一幂等记录后续所有 Lua 操作都稳定落到同一个 slot，
 * 为未来脚本扩展多个关联 Key 留出一致的分片基础。</p>
 */
public final class RedisIdempotencyKeyBuilder {

    private final String prefix;

    public RedisIdempotencyKeyBuilder(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("keyPrefix must not be blank");
        }
        this.prefix = prefix;
    }

    /**
     * @param namespace 业务隔离域
     * @param key       逻辑幂等 Key
     * @return Redis 中实际使用的物理 Key
     */
    public String build(String namespace, String key) {
        return prefix + ":{" + namespace + ":" + key + "}";
    }
}
