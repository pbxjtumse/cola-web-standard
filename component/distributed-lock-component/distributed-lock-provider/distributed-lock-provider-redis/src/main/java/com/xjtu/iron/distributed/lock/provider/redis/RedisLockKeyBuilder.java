package com.xjtu.iron.distributed.lock.provider.redis;

/**
 * Redis 锁 key 构造器。
 *
 * <p>Redis 的 lockKey、fencingKey、releaseChannel 都属于 Redis Provider 的物理命名规则，不应该放到通用
 * LockProvider SPI 中。</p>
 */
public final class RedisLockKeyBuilder {

    /** Redis key 前缀。 */
    private final String keyPrefix;

    /** Redis 释放通知 channel 前缀。 */
    private final String releaseChannelPrefix;

    /** fencing key 后缀。 */
    private final String fencingKeySuffix;

    public RedisLockKeyBuilder() {
        this(RedisLockConstants.DEFAULT_KEY_PREFIX,
                RedisLockConstants.DEFAULT_RELEASE_CHANNEL_PREFIX,
                RedisLockConstants.DEFAULT_FENCING_KEY_SUFFIX);
    }

    public RedisLockKeyBuilder(String keyPrefix, String releaseChannelPrefix, String fencingKeySuffix) {
        this.keyPrefix = trimColon(requireText(keyPrefix, "keyPrefix"));
        this.releaseChannelPrefix = trimColon(requireText(releaseChannelPrefix, "releaseChannelPrefix"));
        this.fencingKeySuffix = trimColon(requireText(fencingKeySuffix, "fencingKeySuffix"));
    }

    /**
     * 构造 Redis 锁 key。
     */
    public String buildLockKey(String namespace, String lockName) {
        return keyPrefix + ':' + normalize(namespace) + ':' + normalize(lockName);
    }

    /**
     * 构造 Redis fencing token key。
     */
    public String buildFencingKey(String namespace, String lockName) {
        return buildLockKey(namespace, lockName) + ':' + fencingKeySuffix;
    }

    /**
     * 构造 Redis 解锁发布 channel。
     */
    public String buildReleaseChannel(String namespace, String lockName) {
        return releaseChannelPrefix + ':' + normalize(namespace) + ':' + normalize(lockName);
    }

    private static String normalize(String value) {
        return trimColon(requireText(value, "redis key part"));
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
