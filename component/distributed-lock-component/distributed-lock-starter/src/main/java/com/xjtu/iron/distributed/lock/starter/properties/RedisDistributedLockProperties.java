package com.xjtu.iron.distributed.lock.starter.properties;

import com.xjtu.iron.distributed.lock.provider.redis.RedisLockConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Redis Provider 配置。 */
@ConfigurationProperties(prefix = "iron.distributed-lock.redis")
public class RedisDistributedLockProperties {
    private boolean enabled = true;
    private String keyPrefix = RedisLockConstants.DEFAULT_KEY_PREFIX;
    private String releaseChannelPrefix = RedisLockConstants.DEFAULT_RELEASE_CHANNEL_PREFIX;
    private String fencingKeySuffix = RedisLockConstants.DEFAULT_FENCING_KEY_SUFFIX;
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }
    public String getReleaseChannelPrefix() { return releaseChannelPrefix; }
    public void setReleaseChannelPrefix(String releaseChannelPrefix) { this.releaseChannelPrefix = releaseChannelPrefix; }
    public String getFencingKeySuffix() { return fencingKeySuffix; }
    public void setFencingKeySuffix(String fencingKeySuffix) { this.fencingKeySuffix = fencingKeySuffix; }
}
