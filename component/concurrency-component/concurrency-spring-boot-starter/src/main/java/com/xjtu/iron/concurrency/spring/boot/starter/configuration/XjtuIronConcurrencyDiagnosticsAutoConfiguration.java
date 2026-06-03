package com.xjtu.iron.concurrency.spring.boot.starter.configuration;

import com.xjtu.iron.concurrency.core.execution.ThreadPoolRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

@AutoConfiguration(after = XjtuIronConcurrencyExecutionAutoConfiguration.class)
public class XjtuIronConcurrencyDiagnosticsAutoConfiguration {

    @Bean(name = "ironConcurrencyHealthIndicator")
    @ConditionalOnClass(name = "org.springframework.boot.actuate.health.HealthIndicator")
    @ConditionalOnMissingBean(name = "ironConcurrencyHealthIndicator")
    public HealthIndicator concurrencyHealthIndicator(ThreadPoolRegistry threadPoolRegistry) {
        return () -> {
            Map<String, Object> details = new LinkedHashMap<>();

            for (Map.Entry<String, ExecutorService> entry : threadPoolRegistry.snapshot().entrySet()) {
                String poolName = entry.getKey();
                ExecutorService executorService = entry.getValue();

                if (!(executorService instanceof ThreadPoolExecutor executor)) {
                    continue;
                }

                Map<String, Object> poolDetail = new LinkedHashMap<>();
                poolDetail.put("corePoolSize", executor.getCorePoolSize());
                poolDetail.put("maximumPoolSize", executor.getMaximumPoolSize());
                poolDetail.put("poolSize", executor.getPoolSize());
                poolDetail.put("activeCount", executor.getActiveCount());
                poolDetail.put("queueSize", executor.getQueue().size());
                poolDetail.put("queueRemainingCapacity", executor.getQueue().remainingCapacity());
                poolDetail.put("completedTaskCount", executor.getCompletedTaskCount());
                poolDetail.put("taskCount", executor.getTaskCount());
                poolDetail.put("shutdown", executor.isShutdown());
                poolDetail.put("terminated", executor.isTerminated());

                details.put(poolName, poolDetail);
            }

            return Health.up()
                    .withDetail("component", "xjtu-iron-concurrency")
                    .withDetail("threadPools", details)
                    .build();
        };
    }
}