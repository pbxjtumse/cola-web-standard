package com.xjtu.iron.distributed.lock.starter.configuration;

import com.xjtu.iron.distributed.lock.core.fencing.FencingTokenProviderRegistry;
import com.xjtu.iron.distributed.lock.core.spi.LockProviderRegistry;
import com.xjtu.iron.distributed.lock.starter.health.DistributedLockHealthIndicator;
import com.xjtu.iron.distributed.lock.starter.properties.DistributedLockProperties;
import com.xjtu.iron.distributed.lock.starter.properties.JdbcFencingTokenProperties;
import com.xjtu.iron.distributed.lock.starter.properties.RedisDistributedLockProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/** Actuator 健康检查自动配置。 */
@AutoConfiguration(after = DistributedLockAutoConfiguration.class)
@ConditionalOnClass(HealthIndicator.class)
public class DistributedLockActuatorAutoConfiguration {

    @Bean
    @ConditionalOnBean(LockProviderRegistry.class)
    @ConditionalOnMissingBean(name = "distributedLockHealthIndicator")
    public DistributedLockHealthIndicator distributedLockHealthIndicator(
            LockProviderRegistry providerRegistry,
            FencingTokenProviderRegistry fencingRegistry,
            DistributedLockProperties properties,
            ObjectProvider<RedisDistributedLockProperties> redisPropertiesProvider,
            ObjectProvider<JdbcFencingTokenProperties> jdbcPropertiesProvider
    ) {
        return new DistributedLockHealthIndicator(
                providerRegistry,
                fencingRegistry,
                properties,
                redisPropertiesProvider.getIfAvailable(),
                jdbcPropertiesProvider.getIfAvailable());
    }
}
