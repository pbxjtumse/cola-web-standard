package com.xjtu.iron.retry.config;

import com.xjtu.iron.retry.api.RetryExecutor;
import com.xjtu.iron.retry.api.RetryIdGenerator;
import com.xjtu.iron.retry.api.RetryListener;
import com.xjtu.iron.retry.api.RetryPolicy;
import com.xjtu.iron.retry.api.RetryPolicyRegistry;
import com.xjtu.iron.retry.core.DefaultRetryExecutor;
import com.xjtu.iron.retry.core.DefaultRetryPolicyRegistry;
import com.xjtu.iron.retry.core.UuidRetryIdGenerator;
import com.xjtu.iron.retry.core.time.RetryClock;
import com.xjtu.iron.retry.core.time.RetrySleeper;
import com.xjtu.iron.retry.core.time.SystemRetryClock;
import com.xjtu.iron.retry.core.time.ThreadSleepRetrySleeper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Map;

/** 装配 retry-component 的核心 Spring Boot Bean。 */
@AutoConfiguration
@EnableConfigurationProperties(RetryProperties.class)
@ConditionalOnProperty(
        prefix = "iron.retry",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class RetryAutoConfiguration {

    /** 在启用 Spring 事件桥接时注册默认监听器。 */
    @Bean
    @ConditionalOnProperty(
            prefix = "iron.retry",
            name = "publish-spring-events",
            havingValue = "true",
            matchIfMissing = true
    )
    @ConditionalOnMissingBean(SpringApplicationRetryListener.class)
    public SpringApplicationRetryListener springApplicationRetryListener(
            ApplicationEventPublisher publisher) {
        return new SpringApplicationRetryListener(publisher);
    }

    /** 注册可被业务覆盖的系统时钟实现。 */
    @Bean
    @ConditionalOnMissingBean
    public RetryClock retryClock() {
        return new SystemRetryClock();
    }

    /** 注册可被业务覆盖的同步等待实现。 */
    @Bean
    @ConditionalOnMissingBean
    public RetrySleeper retrySleeper() {
        return new ThreadSleepRetrySleeper();
    }

    /** 注册可被业务覆盖的逻辑执行标识生成器。 */
    @Bean
    @ConditionalOnMissingBean
    public RetryIdGenerator retryIdGenerator() {
        return new UuidRetryIdGenerator();
    }

    /** 解析全部命名策略并注册到默认策略注册表。 */
    @Bean
    @ConditionalOnMissingBean
    public RetryPolicyRegistry retryPolicyRegistry(RetryProperties properties) {
        DefaultRetryPolicyRegistry registry = new DefaultRetryPolicyRegistry();
        RetryPolicyPropertiesResolver resolver = new RetryPolicyPropertiesResolver();
        RetryPolicyFactory factory = new RetryPolicyFactory();
        Map<String, ResolvedRetryPolicyProperties> resolvedPolicies = resolver.resolve(properties);
        for (Map.Entry<String, ResolvedRetryPolicyProperties> entry
                : resolvedPolicies.entrySet()) {
            RetryPolicy retryPolicy = factory.create(entry.getKey(), entry.getValue());
            registry.register(retryPolicy);
        }
        return registry;
    }

    /** 汇总有序监听器和可替换基础设施后创建默认执行器。 */
    @Bean
    @ConditionalOnMissingBean
    public RetryExecutor retryExecutor(
            RetryPolicyRegistry retryPolicyRegistry,
            ObjectProvider<RetryListener> retryListeners,
            RetrySleeper retrySleeper,
            RetryClock retryClock,
            RetryIdGenerator retryIdGenerator) {
        List<RetryListener> listeners = retryListeners.orderedStream().toList();
        return new DefaultRetryExecutor(
                retryPolicyRegistry,
                listeners,
                retrySleeper,
                retryClock,
                retryIdGenerator
        );
    }
}
