package com.xjtu.iron.distributed.lock.provider.redisson;

/**
 * Redisson 锁对象名称构造器。
 *
 * <p>业务 lockName 不直接作为 Redis 物理 key，统一增加组件前缀和 namespace。
 * 保留 hash tag 便于 Redis Cluster 下 Redisson FencedLock 的锁数据和 token 数据落在兼容槽位。</p>
 */
public final class RedissonLockKeyBuilder {

    private final String keyPrefix;

    public RedissonLockKeyBuilder() {
        this(RedissonLockConstants.DEFAULT_KEY_PREFIX);
    }

    public RedissonLockKeyBuilder(String keyPrefix) {
        this.keyPrefix = trimColon(requireText(keyPrefix, "keyPrefix"));
    }

    public String buildLockKey(String namespace, String lockName) {
        return keyPrefix + ":{" + normalize(namespace) + ':' + normalize(lockName) + "}";
    }

    private static String normalize(String value) {
        return trimColon(requireText(value, "lock key part"));
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String trimColon(String value) {
        String result = value;
        while (result.startsWith(":")) {
            result = result.substring(1);
        }
        while (result.endsWith(":")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
