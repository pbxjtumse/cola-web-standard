package com.xjtu.iron.distributed.lock.starter.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 分布式锁核心配置。 */
@ConfigurationProperties(prefix = "iron.distributed-lock")
public class DistributedLockProperties {
    private boolean enabled = true;
    private String defaultProvider = "redis";
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getDefaultProvider() { return defaultProvider; }
    public void setDefaultProvider(String defaultProvider) { this.defaultProvider = defaultProvider; }
}
