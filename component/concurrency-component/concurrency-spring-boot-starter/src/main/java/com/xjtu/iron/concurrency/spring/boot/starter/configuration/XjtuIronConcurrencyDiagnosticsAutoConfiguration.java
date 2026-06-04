package com.xjtu.iron.concurrency.spring.boot.starter.configuration;

import com.xjtu.iron.concurrency.api.execution.ThreadPoolManager;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = XjtuIronConcurrencyExecutionAutoConfiguration.class)
public class XjtuIronConcurrencyDiagnosticsAutoConfiguration {

    @Bean(name = "ironConcurrencyHealthIndicator")
    @ConditionalOnClass(name = "org.springframework.boot.actuate.health.HealthIndicator")
    @ConditionalOnMissingBean(name = "ironConcurrencyHealthIndicator")
    public HealthIndicator concurrencyHealthIndicator(ThreadPoolManager threadPoolManager) {
        return () -> Health.up()
                .withDetail("component", "xjtu-iron-concurrency")
                .withDetail("threadPools", threadPoolManager.snapshots())
                .build();
    }
}
