package com.xjtu.iron.distributed.lock.starter.properties;

/**
 * Redis 分布式锁配置属性。
 *
 * <p>正式接入 Spring Boot 时，本类通常作为 DistributedLockProperties 的 redis 子配置。</p>
 */
public final class RedisDistributedLockProperties {

    /** 是否启用 Redis Provider。 */
    private boolean enabled = true;

    /** Redis 锁 key 前缀。 */
    private String keyPrefix = "iron:lock";

    /** Redis 释放通知 channel 前缀。二期 PUBSUB_BACKOFF 使用。 */
    private String releaseChannelPrefix = "iron:lock:release";

    /** fencing key 后缀。二期 fencing token 使用。 */
    private String fencingKeySuffix = "fence";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public String getReleaseChannelPrefix() {
        return releaseChannelPrefix;
    }

    public void setReleaseChannelPrefix(String releaseChannelPrefix) {
        this.releaseChannelPrefix = releaseChannelPrefix;
    }

    public String getFencingKeySuffix() {
        return fencingKeySuffix;
    }

    public void setFencingKeySuffix(String fencingKeySuffix) {
        this.fencingKeySuffix = fencingKeySuffix;
    }
}
