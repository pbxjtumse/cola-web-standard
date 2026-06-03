package com.xjtu.iron.concurrency.spring.boot.starter.resolver;

import com.xjtu.iron.concurrency.api.execution.ThreadPoolSpec;
import com.xjtu.iron.concurrency.config.ThreadPoolSpecResolver;
import com.xjtu.iron.concurrency.spring.boot.starter.properties.XjtuIronConcurrencyProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 基于 Spring Boot 配置属性的线程池配置解析器。
 */
public class PropertiesThreadPoolSpecResolver implements ThreadPoolSpecResolver {

    private final XjtuIronConcurrencyProperties properties;

    public PropertiesThreadPoolSpecResolver(XjtuIronConcurrencyProperties properties) {
        this.properties = properties;
    }

    @Override
    public ThreadPoolSpec resolve(String poolName) {
        return resolveAll().get(poolName);
    }

    @Override
    public Map<String, ThreadPoolSpec> resolveAll() {
        Map<String, ThreadPoolSpec> result = new LinkedHashMap<>();

        if (properties.getThreadPools() == null || properties.getThreadPools().isEmpty()) {
            ThreadPoolSpec defaultSpec = new ThreadPoolSpec();
            defaultSpec.setName(properties.getDefaultExecutor());
            defaultSpec.validate();

            result.put(defaultSpec.getName(), defaultSpec);
            return result;
        }

        for (Map.Entry<String, XjtuIronConcurrencyProperties.ThreadPoolProperties> entry
                : properties.getThreadPools().entrySet()) {

            String poolName = entry.getKey();
            XjtuIronConcurrencyProperties.ThreadPoolProperties poolProperties = entry.getValue();

            ThreadPoolSpec spec = new ThreadPoolSpec();
            spec.setName(poolName);
            spec.setCorePoolSize(poolProperties.getCorePoolSize());
            spec.setMaxPoolSize(poolProperties.getMaxPoolSize());
            spec.setQueueCapacity(poolProperties.getQueueCapacity());
            spec.setKeepAliveTime(poolProperties.getKeepAliveTime());
            spec.setAllowCoreThreadTimeout(poolProperties.isAllowCoreThreadTimeout());
            spec.setThreadNamePrefix(poolProperties.getThreadNamePrefix());
            spec.setQueueType(poolProperties.getQueueType());
            spec.setRejectionPolicy(poolProperties.getRejectionPolicy());

            if (poolProperties.getRejectionWaitTime() != null) {
                spec.setRejectionWaitTime(poolProperties.getRejectionWaitTime());
            } else {
                spec.setRejectionWaitTime(properties.getRejectionWaitTime());
            }

            spec.setWaitForTasksToCompleteOnShutdown(poolProperties.isWaitForTasksToCompleteOnShutdown());
            spec.setAwaitTermination(poolProperties.getAwaitTermination());

            spec.validate();

            result.put(poolName, spec);
        }

        return result;
    }
}