package com.xjtu.iron.distributed.lock.provider.redis;

/**
 * Redis 分布式锁常量。
 */
public final class RedisLockConstants {

    /** Redis Provider 名称。 */
    public static final String PROVIDER_NAME = "redis";

    /** 默认 Redis 锁 key 前缀。 */
    public static final String DEFAULT_KEY_PREFIX = "iron:lock";

    /** 默认释放通知 channel 前缀。 */
    public static final String DEFAULT_RELEASE_CHANNEL_PREFIX = "iron:lock:release";

    /** 默认 fencing key 后缀。 */
    public static final String DEFAULT_FENCING_KEY_SUFFIX = "fence";

    private RedisLockConstants() {
    }
}
