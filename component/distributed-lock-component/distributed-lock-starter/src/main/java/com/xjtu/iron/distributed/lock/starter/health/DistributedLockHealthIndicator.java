package com.xjtu.iron.distributed.lock.starter.health;

import com.xjtu.iron.distributed.lock.core.spi.LockProvider;
import com.xjtu.iron.distributed.lock.core.spi.LockProviderCapabilities;
import com.xjtu.iron.distributed.lock.core.spi.LockProviderRegistry;
import com.xjtu.iron.distributed.lock.starter.properties.DistributedLockProperties;
import com.xjtu.iron.distributed.lock.starter.properties.RedisDistributedLockProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * 分布式锁组件健康检查。
 *
 * <p>
 * 该健康检查只确认组件装配状态和默认 Provider 是否可用，不在健康检查里创建临时锁，
 * 避免健康检查对 Redis 写入锁 key 或引入额外竞争。
 * </p>
 */
public final class DistributedLockHealthIndicator implements HealthIndicator {

    private final LockProviderRegistry providerRegistry;
    private final DistributedLockProperties properties;
    private final RedisDistributedLockProperties redisProperties;

    public DistributedLockHealthIndicator(
            LockProviderRegistry providerRegistry,
            DistributedLockProperties properties,
            RedisDistributedLockProperties redisProperties
    ) {
        this.providerRegistry = providerRegistry;
        this.properties = properties;
        this.redisProperties = redisProperties;
    }

    @Override
    public Health health() {
        if (!properties.isEnabled()) {
            return Health.up()
                    .withDetail("enabled", false)
                    .withDetail("message", "distributed lock auto configuration is disabled")
                    .build();
        }

        String defaultProvider = properties.getDefaultProvider();
        try {
            LockProvider provider = providerRegistry.getDefaultProvider();
            LockProviderCapabilities capabilities = provider.capabilities();
            return Health.up()
                    .withDetail("enabled", true)
                    .withDetail("defaultProvider", defaultProvider)
                    .withDetail("actualProvider", provider.providerName())
                    .withDetail("redisEnabled", redisProperties == null || redisProperties.isEnabled())
                    .withDetail("keyPrefix", redisProperties == null ? null : redisProperties.getKeyPrefix())
                    .withDetail("autoRenewSupported", capabilities.isAutoRenewSupported())
                    .withDetail("fencingTokenSupported", capabilities.isFencingTokenSupported())
                    .withDetail("pubSubWaitSupported", capabilities.isPubSubWaitSupported())
                    .withDetail("fairLockSupported", capabilities.isFairLockSupported())
                    .withDetail("reentrantSupported", capabilities.isReentrantSupported())
                    .build();
        } catch (Throwable ex) {
            return Health.down(ex)
                    .withDetail("enabled", true)
                    .withDetail("defaultProvider", defaultProvider)
                    .build();
        }
    }
}
