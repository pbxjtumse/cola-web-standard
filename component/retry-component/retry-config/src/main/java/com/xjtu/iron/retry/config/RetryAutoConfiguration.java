package com.xjtu.iron.retry.config;

import com.xjtu.iron.retry.api.BackoffStrategy;
import com.xjtu.iron.retry.api.RetryExecutor;
import com.xjtu.iron.retry.api.RetryListener;
import com.xjtu.iron.retry.api.RetryPolicy;
import com.xjtu.iron.retry.api.RetryPolicyRegistry;
import com.xjtu.iron.retry.api.support.BackoffStrategies;
import com.xjtu.iron.retry.core.DefaultRetryExecutor;
import com.xjtu.iron.retry.core.DefaultRetryPolicyRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Map;

/**
 * 重试组件核心 Spring Boot 自动配置。
 */
@AutoConfiguration
@EnableConfigurationProperties(RetryProperties.class)
@ConditionalOnProperty(prefix = "iron.retry", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RetryAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RetryPolicyRegistry retryPolicyRegistry(RetryProperties retryProperties) {
        DefaultRetryPolicyRegistry registry = new DefaultRetryPolicyRegistry();
        Map<String, RetryProperties.PolicyProperties> configuredPolicies = retryProperties.getPolicies();

        for (Map.Entry<String, RetryProperties.PolicyProperties> entry : configuredPolicies.entrySet()) {
            registry.register(toRetryPolicy(entry.getKey(), entry.getValue()));
        }

        if (registry.find(retryProperties.getDefaultPolicy()).isEmpty()) {
            registry.register(RetryPolicy.builder(retryProperties.getDefaultPolicy()).build());
        }

        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    public RetryExecutor retryExecutor(
            RetryPolicyRegistry retryPolicyRegistry,
            ObjectProvider<RetryListener> retryListeners) {
        List<RetryListener> listeners = retryListeners.orderedStream().toList();
        return new DefaultRetryExecutor(retryPolicyRegistry, listeners);
    }

    private RetryPolicy toRetryPolicy(
            String policyName,
            RetryProperties.PolicyProperties properties) {
        RetryPolicy.Builder builder = RetryPolicy.builder(policyName)
                .maxAttempts(properties.getMaxAttempts())
                .maxDuration(properties.getMaxDuration())
                .operationSafety(properties.getOperationSafety())
                .backoffStrategy(toBackoffStrategy(properties.getBackoff()));

        for (String exceptionTypeName : properties.getRetryOn()) {
            builder.retryOn(resolveThrowableType(exceptionTypeName));
        }

        for (String exceptionTypeName : properties.getStopOn()) {
            builder.stopOn(resolveThrowableType(exceptionTypeName));
        }

        return builder.build();
    }

    private BackoffStrategy toBackoffStrategy(RetryProperties.BackoffProperties properties) {
        return switch (properties.getType()) {
            case NONE -> BackoffStrategies.none();
            case FIXED -> BackoffStrategies.fixed(properties.getDelay());
            case EXPONENTIAL -> BackoffStrategies.exponential(
                    properties.getInitialDelay(),
                    properties.getMaxDelay(),
                    properties.getMultiplier()
            );
            case EXPONENTIAL_JITTER -> BackoffStrategies.exponentialWithFullJitter(
                    properties.getInitialDelay(),
                    properties.getMaxDelay(),
                    properties.getMultiplier()
            );
        };
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Throwable> resolveThrowableType(String className) {
        if (className == null || className.isBlank()) {
            throw new IllegalArgumentException("Configured exception class name must not be blank");
        }
        try {
            Class<?> loadedType = Class.forName(className);
            if (!Throwable.class.isAssignableFrom(loadedType)) {
                throw new IllegalArgumentException("Configured type is not a Throwable: " + className);
            }
            return (Class<? extends Throwable>) loadedType;
        } catch (ClassNotFoundException exception) {
            throw new IllegalArgumentException("Configured exception type does not exist: " + className, exception);
        }
    }
}
