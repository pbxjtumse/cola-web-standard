package com.xjtu.iron.distributed.lock.starter.properties;

import com.xjtu.iron.distributed.lock.api.LockOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 分布式锁核心配置。
 *
 * <p>
 * 该配置只描述分布式锁组件自身语义，不描述 Redis 连接地址。
 * Redis host/port/password/database/timeout 仍然使用 Spring Boot 标准
 * {@code spring.data.redis.*} 配置。
 * </p>
 */
@ConfigurationProperties(prefix = "iron.distributed-lock")
public class DistributedLockProperties {

    /** 是否启用分布式锁自动配置。 */
    private boolean enabled = true;

    /** 默认 Provider 名称。 */
    private String defaultProvider = "redis";

    /** 默认锁命名空间。 */
    private String namespace = LockOptions.DEFAULT_NAMESPACE;

    /** 默认锁租约时间。 */
    private Duration leaseTime = LockOptions.DEFAULT_LEASE_TIME;

    /** 默认等待时间。 */
    private Duration waitTime = LockOptions.DEFAULT_WAIT_TIME;

    /** 默认是否开启 watchdog 自动续期。 */
    private boolean autoRenew = LockOptions.DEFAULT_AUTO_RENEW;

    /** 默认最大自动续期时间。 */
    private Duration maxRenewTime = LockOptions.DEFAULT_MAX_RENEW_TIME;

    /** 明确失锁后，execute 是否返回 LOCK_LOST。 */
    private boolean failOnLockLost = LockOptions.DEFAULT_FAIL_ON_LOCK_LOST;

    public LockOptions toLockOptions() {
        return LockOptions.builder()
                .namespace(namespace)
                .providerName(defaultProvider)
                .leaseTime(leaseTime)
                .waitTime(waitTime)
                .autoRenew(autoRenew)
                .maxRenewTime(maxRenewTime)
                .failOnLockLost(failOnLockLost)
                .build();
    }

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

    public boolean isAutoRenew() {
        return autoRenew;
    }

    public void setAutoRenew(boolean autoRenew) {
        this.autoRenew = autoRenew;
    }

    public Duration getMaxRenewTime() {
        return maxRenewTime;
    }

    public void setMaxRenewTime(Duration maxRenewTime) {
        this.maxRenewTime = maxRenewTime;
    }

    public boolean isFailOnLockLost() {
        return failOnLockLost;
    }

    public void setFailOnLockLost(boolean failOnLockLost) {
        this.failOnLockLost = failOnLockLost;
    }
}
