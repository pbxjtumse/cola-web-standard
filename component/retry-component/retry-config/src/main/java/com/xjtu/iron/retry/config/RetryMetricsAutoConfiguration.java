package com.xjtu.iron.retry.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Micrometer 指标自动配置。
 */
@AutoConfiguration(after = RetryAutoConfiguration.class)
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnBean(MeterRegistry.class)
@ConditionalOnProperty(prefix = "iron.retry", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RetryMetricsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(MicrometerRetryListener.class)
    public MicrometerRetryListener micrometerRetryListener(MeterRegistry meterRegistry) {
        return new MicrometerRetryListener(meterRegistry);
    }
}
