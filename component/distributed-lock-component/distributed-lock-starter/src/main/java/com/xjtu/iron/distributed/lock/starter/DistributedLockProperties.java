package com.xjtu.iron.distributed.lock.starter;

import java.time.Duration;

/**
 * 分布式锁 starter 配置属性占位类。
 *
 * <p>正式接入 Spring Boot 时，本类会配合 {@code @ConfigurationProperties(prefix = "iron.distributed-lock")}
 * 使用。当前阶段先定义字段语义，避免后续 starter 配置名反复变化。</p>
 */
public final class DistributedLockProperties {

    /** 是否启用分布式锁组件。 */
    private boolean enabled = true;

    /** 默认 Provider 名称。 */
    private String defaultProvider = "redis";

    /** 默认命名空间。 */
    private String namespace = "default";

    /** 默认租约时间。 */
    private Duration leaseTime = Duration.ofSeconds(30);

    /** 默认等待时间。 */
    private Duration waitTime = Duration.ZERO;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getDefaultProvider() { return defaultProvider; }
    public void setDefaultProvider(String defaultProvider) { this.defaultProvider = defaultProvider; }
    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }
    public Duration getLeaseTime() { return leaseTime; }
    public void setLeaseTime(Duration leaseTime) { this.leaseTime = leaseTime; }
    public Duration getWaitTime() { return waitTime; }
    public void setWaitTime(Duration waitTime) { this.waitTime = waitTime; }
}
