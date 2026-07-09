package com.xjtu.iron.distributed.lock.starter;

/**
 * Redis 分布式锁配置属性占位类。
 */
public final class RedisDistributedLockProperties {

    /** Redis 锁 key 前缀。 */
    private String keyPrefix = "iron:lock";

    /** Redis 释放通知 channel 前缀。 */
    private String releaseChannelPrefix = "iron:lock:release";

    /** fencing key 后缀。 */
    private String fencingKeySuffix = "fence";

    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }
    public String getReleaseChannelPrefix() { return releaseChannelPrefix; }
    public void setReleaseChannelPrefix(String releaseChannelPrefix) { this.releaseChannelPrefix = releaseChannelPrefix; }
    public String getFencingKeySuffix() { return fencingKeySuffix; }
    public void setFencingKeySuffix(String fencingKeySuffix) { this.fencingKeySuffix = fencingKeySuffix; }
}
