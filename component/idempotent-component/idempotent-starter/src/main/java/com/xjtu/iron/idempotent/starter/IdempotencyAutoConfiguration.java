package com.xjtu.iron.idempotent.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xjtu.iron.distributed.lock.api.DistributedLockClient;
import com.xjtu.iron.idempotent.api.*;
import com.xjtu.iron.idempotent.api.repository.IdempotencyRepository;
import com.xjtu.iron.idempotent.api.spi.IdempotencyFailureClassifier;
import com.xjtu.iron.idempotent.api.spi.IdempotencyRequestHasher;
import com.xjtu.iron.idempotent.api.spi.IdempotencyResultCodec;
import com.xjtu.iron.idempotent.core.*;
import com.xjtu.iron.idempotent.core.observation.IdempotencyEventPublisher;
import com.xjtu.iron.idempotent.core.observation.IdempotencyMetrics;
import com.xjtu.iron.idempotent.core.transaction.IdempotencyTransactionCoordinator;
import com.xjtu.iron.idempotent.integration.transaction.SpringTransactionJdbcExecutionManager;
import com.xjtu.iron.idempotent.integration.transaction.TransactionTemplateIdempotencyTransactionCoordinator;
import com.xjtu.iron.idempotent.provider.jdbc.DataSourceJdbcExecutionManager;
import com.xjtu.iron.idempotent.provider.jdbc.JdbcExecutionManager;
import com.xjtu.iron.idempotent.provider.jdbc.JdbcIdempotencyRepository;
import com.xjtu.iron.idempotent.provider.redis.RedisIdempotencyRepository;
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
import java.util.List;

/**
 * 幂等组件统一自动装配入口。
 *
 * <p>Starter 的职责只有“把外部配置和基础设施 Bean 组装成 Core 所需对象”。
 * 状态机、Repository 选择、execute/recover 主流程都不写在自动配置中。</p>
 *
 * <p>覆盖规则遵循 Spring Boot Starter 常规约定：用户显式提供的 Bean 优先，
 * 组件默认实现通过 {@code @ConditionalOnMissingBean} 补齐。</p>
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

    @Bean
    @ConditionalOnClass(ObjectMapper.class)
    @ConditionalOnMissingBean(IdempotencyResultCodec.class)
    public IdempotencyResultCodec idempotencyResultCodec(ObjectProvider<ObjectMapper> provider) {
        return new JacksonIdempotencyResultCodec(provider.getIfAvailable(ObjectMapper::new));
    }

    @Bean
    @ConditionalOnClass(ObjectMapper.class)
    @ConditionalOnMissingBean(IdempotencyRequestHasher.class)
    public IdempotencyRequestHasher idempotencyRequestHasher(ObjectProvider<ObjectMapper> provider) {
        return new JacksonSha256IdempotencyRequestHasher(provider.getIfAvailable(ObjectMapper::new));
    }

    @Bean
    @ConditionalOnMissingBean(IdempotencyEventPublisher.class)
    public IdempotencyEventPublisher idempotencyEventPublisher(ApplicationEventPublisher publisher) {
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

    /** SHORT_TERM 默认 Redis Repository。连接信息仍复用 spring.data.redis.*。 */
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
        return new RedisIdempotencyRepository(redis, properties.getRedis().getKeyPrefix());
    }

    /**
     * JDBC Connection / 事务参与桥梁。
     *
     * <p>当 transaction-component 的 TransactionExecutor 已存在，并且 transaction.enabled=true 时，
     * 自动切换为 SpringTransactionJdbcExecutionManager：</p>
     * <ul>
     *     <li>tryAcquire / tryRecover / markFailed -> REQUIRES_NEW；</li>
     *     <li>markSuccess -> 复用 Tx-B 当前 transaction-bound Connection。</li>
     * </ul>
     *
     * <p>没有 transaction-component 时继续使用 DataSourceJdbcExecutionManager，保持 V1.1 兼容行为。</p>
     */
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

    /**
     * Tx-B 事务协调器。只有 transaction-component 真正提供 TransactionExecutor Bean 时才创建。
     */
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
        return new TransactionTemplateIdempotencyTransactionCoordinator(transactionExecutor);
    }

    /** DURABLE 默认 JDBC Repository。业务只注入 IdempotencyExecutor，不直接依赖该 Bean。 */
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
        return new JdbcIdempotencyRepository(jdbc, properties.getJdbc().getTableName());
    }

    @Bean
    @ConditionalOnMissingBean
    public IdempotencyRepositoryRegistry idempotencyRepositoryRegistry(
            List<IdempotencyRepository> repositories,
            IdempotencyProperties properties) {
        return new DefaultIdempotencyRepositoryRegistry(
                repositories,
                properties.getDefaultShortTermRepository(),
                properties.getDefaultDurableRepository());
    }

    /**
     * 把 ConfigurationProperties 转换成 Core 使用的不可变 Options。
     *
     * <p>SHORT_TERM 与 DURABLE 拥有不同默认恢复/窗口语义，因此这里分别构建两份快照。</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public IdempotencyDefaults idempotencyDefaults(IdempotencyProperties properties) {
        IdempotencyLockOptions lock = IdempotencyLockOptions.builder()
                .enabled(properties.getLock().isEnabled())
                .providerName(properties.getLock().getProviderName())
                .waitTime(properties.getLock().getWaitTime())
                .leaseTime(properties.getLock().getLeaseTime())
                .fallbackToStateOnFailure(properties.getLock().isFallbackToStateOnFailure())
                .build();

        IdempotencyOptions shortTerm = IdempotencyOptions.builder()
                .mode(IdempotencyMode.SHORT_TERM)
                .processingTimeout(properties.getProcessingTimeout())
                .idempotencyWindow(properties.getShortTerm().getIdempotencyWindow())
                .windowPolicy(properties.getShortTerm().getWindowPolicy())
                .recordRetentionTtl(properties.getShortTerm().getRecordRetentionTtl())
                .recoveryMode(properties.getShortTerm().getRecoveryMode())
                .recoverFailed(false)
                .storeResult(properties.isStoreResult())
                .lockOptions(lock)
                .build();

        IdempotencyOptions durable = IdempotencyOptions.builder()
                .mode(IdempotencyMode.DURABLE)
                .processingTimeout(properties.getProcessingTimeout())
                .recoveryMode(properties.getDurable().getRecoveryMode())
                .recoverFailed(properties.getDurable().isRecoverFailed())
                .storeResult(properties.isStoreResult())
                .lockOptions(lock)
                .build();

        return new IdempotencyDefaults(properties.getDefaultMode(), shortTerm, durable);
    }

    /**
     * 组装业务真正注入使用的统一门面。
     *
     * <p>DistributedLockClient 是 ObjectProvider：未接入分布式锁组件时幂等状态机仍可独立工作。</p>
     */
    @Bean
    @ConditionalOnMissingBean(IdempotencyExecutor.class)
    public IdempotencyExecutor idempotencyExecutor(
            IdempotencyRepositoryRegistry registry,
            IdempotencyDefaults defaults,
            IdempotencyOwnerTokenGenerator ownerGenerator,
            IdempotencyFailureClassifier failureClassifier,
            ObjectProvider<IdempotencyResultCodec> codec,
            ObjectProvider<DistributedLockClient> lockClient,
            ObjectProvider<IdempotencyTransactionCoordinator> transactionCoordinator,
            ObjectProvider<IdempotencyEventPublisher> eventPublisher,
            ObjectProvider<IdempotencyMetrics> metrics,
            Clock clock) {
        return new DefaultIdempotencyExecutor(
                registry,
                defaults,
                ownerGenerator,
                failureClassifier,
                codec.getIfAvailable(),
                lockClient.getIfAvailable(),
                transactionCoordinator.getIfAvailable(),
                eventPublisher.getIfAvailable(IdempotencyEventPublisher::noop),
                metrics.getIfAvailable(IdempotencyMetrics::noop),
                clock);
    }

    @Bean
    @ConditionalOnMissingBean(IdempotencyRecoveryQueryService.class)
    public IdempotencyRecoveryQueryService idempotencyRecoveryQueryService(
            IdempotencyRepositoryRegistry registry) {
        return new DefaultIdempotencyRecoveryQueryService(registry);
    }
}
