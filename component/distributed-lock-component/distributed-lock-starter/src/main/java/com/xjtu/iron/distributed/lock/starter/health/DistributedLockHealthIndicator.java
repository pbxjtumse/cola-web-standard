package com.xjtu.iron.distributed.lock.starter.health;

import com.xjtu.iron.distributed.lock.core.fencing.FencingTokenProviderRegistry;
import com.xjtu.iron.distributed.lock.core.spi.LockProvider;
import com.xjtu.iron.distributed.lock.core.spi.LockProviderCapabilities;
import com.xjtu.iron.distributed.lock.core.spi.LockProviderRegistry;
import com.xjtu.iron.distributed.lock.provider.jdbc.fencing.JdbcFencingTokenConstants;
import com.xjtu.iron.distributed.lock.starter.properties.DistributedLockProperties;
import com.xjtu.iron.distributed.lock.starter.properties.JdbcFencingTokenProperties;
import com.xjtu.iron.distributed.lock.starter.properties.RedisDistributedLockProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * 分布式锁组件健康检查。
 *
 * <p>检查装配状态、能力矩阵和 fencing 配置一致性；不会主动创建锁或递增 token。</p>
 */
public final class DistributedLockHealthIndicator implements HealthIndicator {

    private final LockProviderRegistry providerRegistry;
    private final FencingTokenProviderRegistry fencingRegistry;
    private final DistributedLockProperties properties;
    private final RedisDistributedLockProperties redisProperties;
    private final JdbcFencingTokenProperties jdbcFencingProperties;

    public DistributedLockHealthIndicator(
            LockProviderRegistry providerRegistry,
            FencingTokenProviderRegistry fencingRegistry,
            DistributedLockProperties properties,
            RedisDistributedLockProperties redisProperties,
            JdbcFencingTokenProperties jdbcFencingProperties
    ) {
        this.providerRegistry = providerRegistry;
        this.fencingRegistry = fencingRegistry;
        this.properties = properties;
        this.redisProperties = redisProperties;
        this.jdbcFencingProperties = jdbcFencingProperties;
    }

    @Override
    public Health health() {
        if (!properties.isEnabled()) {
            return Health.up()
                    .withDetail("enabled", false)
                    .withDetail("message", "distributed lock auto configuration is disabled")
                    .build();
        }

        String configuredLockProvider = properties.getDefaultProvider();
        try {
            LockProvider lockProvider = providerRegistry.getDefaultProvider();
            LockProviderCapabilities capabilities = lockProvider.capabilities();
            String configuredFencingProvider = trimToNull(properties.getFencingTokenProviderName());
            boolean jdbcEnabled = jdbcFencingProperties != null && jdbcFencingProperties.isEnabled();
            boolean jdbcProviderRegistered = fencingRegistry.findProvider(
                    JdbcFencingTokenConstants.PROVIDER_NAME).isPresent();
            boolean fencingReady = isFencingReady(
                    lockProvider, capabilities, configuredFencingProvider);
            boolean jdbcConfigurationReady = !jdbcEnabled || jdbcProviderRegistered;

            Health.Builder builder = fencingReady && jdbcConfigurationReady
                    ? Health.up()
                    : Health.down();

            return builder
                    .withDetail("enabled", true)
                    .withDetail("defaultProvider", configuredLockProvider)
                    .withDetail("actualProvider", lockProvider.providerName())
                    .withDetail("redisEnabled", redisProperties == null || redisProperties.isEnabled())
                    .withDetail("keyPrefix", redisProperties == null ? null : redisProperties.getKeyPrefix())
                    .withDetail("fencingRequiredByDefault", properties.isFencingRequired())
                    .withDetail("configuredFencingProvider", configuredFencingProvider)
                    .withDetail("nativeFencingSupported", capabilities.isFencingTokenSupported())
                    .withDetail("externalFencingProviders", fencingRegistry.providerNames())
                    .withDetail("fencingReady", fencingReady)
                    .withDetail("jdbcFencingEnabled", jdbcEnabled)
                    .withDetail("jdbcFencingProviderRegistered", jdbcProviderRegistered)
                    .withDetail("jdbcFencingTable", jdbcFencingProperties == null
                            ? null : jdbcFencingProperties.getTableName())
                    .withDetail("autoRenewSupported", capabilities.isAutoRenewSupported())
                    .withDetail("pubSubWaitSupported", capabilities.isPubSubWaitSupported())
                    .withDetail("fairLockSupported", capabilities.isFairLockSupported())
                    .withDetail("reentrantSupported", capabilities.isReentrantSupported())
                    .build();
        } catch (Throwable ex) {
            return Health.down(ex)
                    .withDetail("enabled", true)
                    .withDetail("defaultProvider", configuredLockProvider)
                    .build();
        }
    }

    private boolean isFencingReady(
            LockProvider lockProvider,
            LockProviderCapabilities capabilities,
            String configuredFencingProvider
    ) {
        if (!properties.isFencingRequired()) {
            return true;
        }
        if (configuredFencingProvider != null) {
            if (configuredFencingProvider.equals(lockProvider.providerName())) {
                return capabilities.isFencingTokenSupported();
            }
            return fencingRegistry.findProvider(configuredFencingProvider).isPresent();
        }
        return capabilities.isFencingTokenSupported()
                || fencingRegistry.defaultProvider().isPresent();
    }

    private static String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
