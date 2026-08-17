package com.xjtu.iron.idempotent.starter.autoconfigure;

import com.xjtu.iron.idempotent.core.execution.DefaultIdempotencyExecutor;
import com.xjtu.iron.idempotent.core.failure.DefaultIdempotencyFailureClassifier;
import com.xjtu.iron.idempotent.core.owner.IdempotencyOwnerTokenGenerator;
import com.xjtu.iron.idempotent.core.owner.UuidIdempotencyOwnerTokenGenerator;
import com.xjtu.iron.idempotent.core.policy.DefaultIdempotencyPolicyRegistry;
import com.xjtu.iron.idempotent.core.policy.IdempotencyPolicyRegistry;
import com.xjtu.iron.idempotent.core.recovery.DefaultIdempotencyRecoveryQueryService;
import com.xjtu.iron.idempotent.core.repository.DefaultIdempotencyRepositoryRegistry;
import com.xjtu.iron.idempotent.core.repository.IdempotencyRepositoryRegistry;
import com.xjtu.iron.idempotent.starter.hash.JacksonSha256IdempotencyRequestHasher;
import com.xjtu.iron.idempotent.starter.observation.MicrometerIdempotencyMetrics;
import com.xjtu.iron.idempotent.starter.observation.SpringIdempotencyEventPublisher;
import com.xjtu.iron.idempotent.starter.properties.IdempotencyProperties;
import com.xjtu.iron.idempotent.starter.result.JacksonIdempotencySnapshotPolicyFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xjtu.iron.distributed.lock.api.DistributedLockClient;
import com.xjtu.iron.idempotent.api.context.*;
import com.xjtu.iron.idempotent.api.execution.*;
import com.xjtu.iron.idempotent.api.policy.*;
import com.xjtu.iron.idempotent.api.recovery.*;
import com.xjtu.iron.idempotent.api.request.*;
import com.xjtu.iron.idempotent.api.result.*;
import com.xjtu.iron.idempotent.api.state.*;
import com.xjtu.iron.idempotent.api.repository.IdempotencyRepository;
import com.xjtu.iron.idempotent.api.result.IdempotencySnapshotPolicyFactory;
import com.xjtu.iron.idempotent.api.spi.IdempotencyFailureClassifier;
import com.xjtu.iron.idempotent.api.spi.IdempotencyRequestHasher;
import com.xjtu.iron.idempotent.core.observation.IdempotencyEventPublisher;
import com.xjtu.iron.idempotent.core.observation.IdempotencyMetrics;
import com.xjtu.iron.idempotent.core.state.DefaultIdempotencyStateMachine;
import com.xjtu.iron.idempotent.core.state.IdempotencyStateMachine;
import com.xjtu.iron.idempotent.core.transaction.IdempotencyTransactionCoordinator;
import com.xjtu.iron.idempotent.integration.transaction.jdbc.SpringTransactionJdbcExecutionManager;
import com.xjtu.iron.idempotent.integration.transaction.coordinator.TransactionTemplateIdempotencyTransactionCoordinator;
import com.xjtu.iron.idempotent.provider.jdbc.execution.DataSourceJdbcExecutionManager;
import com.xjtu.iron.idempotent.provider.jdbc.execution.JdbcExecutionManager;
import com.xjtu.iron.idempotent.provider.jdbc.repository.JdbcIdempotencyRepository;
import com.xjtu.iron.idempotent.provider.redis.repository.RedisIdempotencyRepository;
import com.xjtu.iron.transaction.api.execution.TransactionExecutor;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 幂等组件 V1.3 自动装配入口。
 */
@AutoConfiguration(afterName = "com.xjtu.iron.transaction.starter.autoconfigure.TransactionAutoConfiguration")
@EnableConfigurationProperties(IdempotencyProperties.class)
@ConditionalOnProperty(
        prefix = "xjtu.iron.idempotent",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class IdempotencyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IdempotencyOwnerTokenGenerator idempotencyOwnerTokenGenerator() {
        return new UuidIdempotencyOwnerTokenGenerator();
    }

    @Bean
    @ConditionalOnMissingBean
    public IdempotencyFailureClassifier idempotencyFailureClassifier() {
        return new DefaultIdempotencyFailureClassifier();
    }

    /**
     * 结果快照不再通过 Executor 的 Class<T> 处理。
     * Jackson 只作为 ResultPolicy 的可选类型安全工厂存在。
     */
    @Bean
    @ConditionalOnClass(ObjectMapper.class)
    @ConditionalOnMissingBean(IdempotencySnapshotPolicyFactory.class)
    public IdempotencySnapshotPolicyFactory idempotencySnapshotPolicyFactory(
            ObjectProvider<ObjectMapper> provider) {
        return new JacksonIdempotencySnapshotPolicyFactory(
                provider.getIfAvailable(ObjectMapper::new));
    }

    @Bean
    @ConditionalOnClass(ObjectMapper.class)
    @ConditionalOnMissingBean(IdempotencyRequestHasher.class)
    public IdempotencyRequestHasher idempotencyRequestHasher(
            ObjectProvider<ObjectMapper> provider) {
        return new JacksonSha256IdempotencyRequestHasher(
                provider.getIfAvailable(ObjectMapper::new));
    }

    @Bean
    @ConditionalOnMissingBean(IdempotencyEventPublisher.class)
    public IdempotencyEventPublisher idempotencyEventPublisher(
            ApplicationEventPublisher publisher) {
        return new SpringIdempotencyEventPublisher(publisher);
    }

    @Bean
    @ConditionalOnClass(MeterRegistry.class)
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnMissingBean(IdempotencyMetrics.class)
    public IdempotencyMetrics idempotencyMetrics(MeterRegistry registry) {
        return new MicrometerIdempotencyMetrics(registry);
    }

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock idempotencyClock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean(IdempotencyStateMachine.class)
    public IdempotencyStateMachine idempotencyStateMachine() {
        return new DefaultIdempotencyStateMachine();
    }

    @Bean(name = "redisIdempotencyRepository")
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnProperty(
            prefix = "xjtu.iron.idempotent.redis",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public IdempotencyRepository redisIdempotencyRepository(
            StringRedisTemplate redis,
            IdempotencyProperties properties) {
        return new RedisIdempotencyRepository(
                redis, properties.getRedis().getKeyPrefix());
    }

    @Bean
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnMissingBean(JdbcExecutionManager.class)
    public JdbcExecutionManager idempotencyJdbcExecutionManager(
            DataSource dataSource,
            ObjectProvider<TransactionExecutor> transactionExecutor,
            IdempotencyProperties properties) {

        TransactionExecutor executor = transactionExecutor.getIfAvailable();
        if (properties.getTransaction().isEnabled() && executor != null) {
            return new SpringTransactionJdbcExecutionManager(dataSource, executor);
        }

        if (properties.getTransaction().isEnabled()
                && properties.getTransaction().isRequireTemplate()
                && executor == null) {
            throw new IllegalStateException(
                    "xjtu.iron.idempotent.transaction.require-template=true, "
                            + "but no transaction-component TransactionExecutor bean is available");
        }

        return new DataSourceJdbcExecutionManager(dataSource);
    }

    @Bean
    @ConditionalOnBean(TransactionExecutor.class)
    @ConditionalOnProperty(
            prefix = "xjtu.iron.idempotent.transaction",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    @ConditionalOnMissingBean(IdempotencyTransactionCoordinator.class)
    public IdempotencyTransactionCoordinator idempotencyTransactionCoordinator(
            TransactionExecutor transactionExecutor) {
        return new TransactionTemplateIdempotencyTransactionCoordinator(
                transactionExecutor);
    }

    @Bean(name = "jdbcIdempotencyRepository")
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnProperty(
            prefix = "xjtu.iron.idempotent.jdbc",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public IdempotencyRepository jdbcIdempotencyRepository(
            JdbcExecutionManager jdbc,
            IdempotencyProperties properties) {
        return new JdbcIdempotencyRepository(
                jdbc, properties.getJdbc().getTableName());
    }

    @Bean
    @ConditionalOnMissingBean
    public IdempotencyRepositoryRegistry idempotencyRepositoryRegistry(
            List<IdempotencyRepository> repositories,
            IdempotencyProperties properties) {
        return new DefaultIdempotencyRepositoryRegistry(
                repositories,
                properties.getDefaultWindowedRepository(),
                properties.getDefaultDurableRepository());
    }

    /**
     * 组装两个内建默认 Policy + application.yml 中的自定义命名 Policy。
     */
    @Bean
    @ConditionalOnMissingBean
    public IdempotencyPolicyRegistry idempotencyPolicyRegistry(
            IdempotencyProperties properties) {

        List<IdempotencyPolicy> policies = new ArrayList<>();
        IdempotencyLockOptions globalLock = lockOptions(properties.getLock());

        IdempotencyProperties.Windowed windowed = properties.getWindowed();
        // 内建默认策略可以被同名 application.yml 策略显式覆盖，
        // 避免“用户想调整默认策略却因为重复 name 启动失败”。
        if (!properties.getPolicies().containsKey("windowed-default")) {
            policies.add(IdempotencyPolicy.builder()
                    .name("windowed-default")
                    .mode(IdempotencyMode.WINDOWED)
                    .repositoryName(properties.getDefaultWindowedRepository())
                    .processingTimeout(properties.getProcessingTimeout())
                    .idempotencyWindow(windowed.getIdempotencyWindow())
                    .windowPolicy(windowed.getWindowPolicy())
                    .recordRetentionTtl(windowed.getRecordRetentionTtl())
                    .recoveryPolicy(IdempotencyRecoveryPolicy.builder()
                            .mode(windowed.getRecoveryMode())
                            .recoverProcessingTimeout(windowed.isRecoverProcessingTimeout())
                            .recoverRetryableFailure(windowed.isRecoverFailed())
                            .build())
                    .lockOptions(globalLock)
                    .build());
        }

        IdempotencyProperties.Durable durable = properties.getDurable();
        if (!properties.getPolicies().containsKey("durable-default")) {
            policies.add(IdempotencyPolicy.builder()
                    .name("durable-default")
                    .mode(IdempotencyMode.DURABLE)
                    .repositoryName(properties.getDefaultDurableRepository())
                    .processingTimeout(properties.getProcessingTimeout())
                    .recoveryPolicy(IdempotencyRecoveryPolicy.builder()
                            .mode(durable.getRecoveryMode())
                            .recoverProcessingTimeout(durable.isRecoverProcessingTimeout())
                            .recoverRetryableFailure(durable.isRecoverFailed())
                            .build())
                    .lockOptions(globalLock)
                    .build());
        }

        for (Map.Entry<String, IdempotencyProperties.Policy> entry
                : properties.getPolicies().entrySet()) {
            policies.add(buildNamedPolicy(
                    entry.getKey(),
                    entry.getValue(),
                    properties,
                    globalLock));
        }

        return new DefaultIdempotencyPolicyRegistry(
                policies,
                properties.getDefaultPolicy());
    }

    @Bean
    @ConditionalOnMissingBean(IdempotencyExecutor.class)
    public IdempotencyExecutor idempotencyExecutor(
            IdempotencyRepositoryRegistry repositoryRegistry,
            IdempotencyPolicyRegistry policyRegistry,
            IdempotencyOwnerTokenGenerator ownerGenerator,
            IdempotencyFailureClassifier failureClassifier,
            ObjectProvider<DistributedLockClient> lockClient,
            ObjectProvider<IdempotencyTransactionCoordinator> transactionCoordinator,
            IdempotencyStateMachine stateMachine,
            ObjectProvider<IdempotencyEventPublisher> eventPublisher,
            ObjectProvider<IdempotencyMetrics> metrics,
            Clock clock) {

        return new DefaultIdempotencyExecutor(
                repositoryRegistry,
                policyRegistry,
                ownerGenerator,
                failureClassifier,
                lockClient.getIfAvailable(),
                transactionCoordinator.getIfAvailable(),
                stateMachine,
                eventPublisher.getIfAvailable(IdempotencyEventPublisher::noop),
                metrics.getIfAvailable(IdempotencyMetrics::noop),
                clock);
    }

    @Bean
    @ConditionalOnMissingBean(IdempotencyRecoveryQueryService.class)
    public IdempotencyRecoveryQueryService idempotencyRecoveryQueryService(
            IdempotencyRepositoryRegistry registry,
            IdempotencyPolicyRegistry policyRegistry) {
        return new DefaultIdempotencyRecoveryQueryService(registry, policyRegistry);
    }

    private IdempotencyPolicy buildNamedPolicy(
            String name,
            IdempotencyProperties.Policy source,
            IdempotencyProperties root,
            IdempotencyLockOptions globalLock) {

        IdempotencyMode mode;
        if (source.getMode() != null) {
            mode = source.getMode().canonical();
        } else if ("windowed-default".equals(name)) {
            mode = IdempotencyMode.WINDOWED;
        } else {
            // durable-default 以及普通自定义 Policy 在未声明 mode 时都以 DURABLE 为安全默认。
            mode = IdempotencyMode.DURABLE;
        }
        boolean windowed = mode.isWindowed();

        Duration processingTimeout = source.getProcessingTimeout() == null
                ? root.getProcessingTimeout() : source.getProcessingTimeout();

        IdempotencyRecoveryMode recoveryMode = source.getRecoveryMode();
        if (recoveryMode == null) {
            recoveryMode = windowed
                    ? root.getWindowed().getRecoveryMode()
                    : root.getDurable().getRecoveryMode();
        }

        boolean recoverProcessing = source.getRecoverProcessingTimeout() != null
                ? source.getRecoverProcessingTimeout()
                : (windowed
                    ? root.getWindowed().isRecoverProcessingTimeout()
                    : root.getDurable().isRecoverProcessingTimeout());

        boolean recoverFailed = source.getRecoverFailed() != null
                ? source.getRecoverFailed()
                : (windowed
                    ? root.getWindowed().isRecoverFailed()
                    : root.getDurable().isRecoverFailed());

        IdempotencyPolicy.Builder builder = IdempotencyPolicy.builder()
                .name(name)
                .mode(mode)
                .namespace(source.getNamespace())
                .repositoryName(source.getRepositoryName() == null
                        ? (windowed
                            ? root.getDefaultWindowedRepository()
                            : root.getDefaultDurableRepository())
                        : source.getRepositoryName())
                .processingTimeout(processingTimeout)
                .recoveryPolicy(IdempotencyRecoveryPolicy.builder()
                        .mode(recoveryMode)
                        .recoverProcessingTimeout(recoverProcessing)
                        .recoverRetryableFailure(recoverFailed)
                        .build())
                .lockOptions(mergeLock(source, globalLock));

        if (windowed) {
            builder.idempotencyWindow(
                            source.getIdempotencyWindow() == null
                                    ? root.getWindowed().getIdempotencyWindow()
                                    : source.getIdempotencyWindow())
                    .windowPolicy(
                            source.getWindowPolicy() == null
                                    ? root.getWindowed().getWindowPolicy()
                                    : source.getWindowPolicy())
                    .recordRetentionTtl(
                            source.getRecordRetentionTtl() == null
                                    ? root.getWindowed().getRecordRetentionTtl()
                                    : source.getRecordRetentionTtl());
        }

        return builder.build();
    }

    private IdempotencyLockOptions mergeLock(
            IdempotencyProperties.Policy policy,
            IdempotencyLockOptions global) {

        boolean enabled = policy.getLockEnabled() == null
                ? global.isEnabled() : policy.getLockEnabled();

        return IdempotencyLockOptions.builder()
                .enabled(enabled)
                .providerName(policy.getLockProviderName() == null
                        ? global.getProviderName()
                        : policy.getLockProviderName())
                .waitTime(policy.getLockWaitTime() == null
                        ? global.getWaitTime()
                        : policy.getLockWaitTime())
                .leaseTime(policy.getLockLeaseTime() == null
                        ? global.getLeaseTime()
                        : policy.getLockLeaseTime())
                .fallbackToStateOnFailure(
                        policy.getLockFallbackToStateOnFailure() == null
                                ? global.isFallbackToStateOnFailure()
                                : policy.getLockFallbackToStateOnFailure())
                .build();
    }

    private IdempotencyLockOptions lockOptions(IdempotencyProperties.Lock lock) {
        return IdempotencyLockOptions.builder()
                .enabled(lock.isEnabled())
                .providerName(lock.getProviderName())
                .waitTime(lock.getWaitTime())
                .leaseTime(lock.getLeaseTime())
                .fallbackToStateOnFailure(lock.isFallbackToStateOnFailure())
                .build();
    }
}
