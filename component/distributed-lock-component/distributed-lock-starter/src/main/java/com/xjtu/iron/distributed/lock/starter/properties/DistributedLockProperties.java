package com.xjtu.iron.distributed.lock.starter.properties;

import com.xjtu.iron.distributed.lock.api.LockWaitStrategy;

import java.time.Duration;

/**
 * 分布式锁 starter 配置属性。
 *
 * <p>正式接入 Spring Boot 时，本类应配合
 * {@code @ConfigurationProperties(prefix = "iron.distributed-lock")} 使用。
 * 当前阶段先固定配置字段语义，避免后续 starter 配置名反复变化。</p>
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

    /** 默认等待策略。为空时可由 LockOptions 根据 waitTime 自动推导。 */
    private LockWaitStrategy waitStrategy;

    /** 是否默认开启自动续期。 */
    private boolean autoRenew;

    /** 默认续期间隔。 */
    private Duration renewInterval = Duration.ofSeconds(10);

    /** 默认最大自动续期时间。 */
    private Duration maxRenewTime = Duration.ofMinutes(10);

    /** 是否默认要求 fencing token。二期 fencing 能力启用后使用。 */
    private boolean fencingRequired;

    /** 失锁后 execute 模板是否默认返回 LOCK_LOST。 */
    private boolean failOnLockLost;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDefaultProvider() {
        return defaultProvider;
    }

    public void setDefaultProvider(String defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Duration getLeaseTime() {
        return leaseTime;
    }

    public void setLeaseTime(Duration leaseTime) {
        this.leaseTime = leaseTime;
    }

    public Duration getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(Duration waitTime) {
        this.waitTime = waitTime;
    }

    public LockWaitStrategy getWaitStrategy() {
        return waitStrategy;
    }

    public void setWaitStrategy(LockWaitStrategy waitStrategy) {
        this.waitStrategy = waitStrategy;
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }

    public void setAutoRenew(boolean autoRenew) {
        this.autoRenew = autoRenew;
    }

    public Duration getRenewInterval() {
        return renewInterval;
    }

    public void setRenewInterval(Duration renewInterval) {
        this.renewInterval = renewInterval;
    }

    public Duration getMaxRenewTime() {
        return maxRenewTime;
    }

    public void setMaxRenewTime(Duration maxRenewTime) {
        this.maxRenewTime = maxRenewTime;
    }

    public boolean isFencingRequired() {
        return fencingRequired;
    }

    public void setFencingRequired(boolean fencingRequired) {
        this.fencingRequired = fencingRequired;
    }

    public boolean isFailOnLockLost() {
        return failOnLockLost;
    }

    public void setFailOnLockLost(boolean failOnLockLost) {
        this.failOnLockLost = failOnLockLost;
    }
}
