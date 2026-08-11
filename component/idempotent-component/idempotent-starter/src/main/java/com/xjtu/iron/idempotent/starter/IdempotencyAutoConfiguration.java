package com.xjtu.iron.idempotent.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xjtu.iron.distributed.lock.api.DistributedLockClient;
import com.xjtu.iron.idempotent.api.IdempotencyExecutor;
import com.xjtu.iron.idempotent.api.IdempotencyLockOptions;
import com.xjtu.iron.idempotent.api.IdempotencyMode;
import com.xjtu.iron.idempotent.api.IdempotencyOptions;
import com.xjtu.iron.idempotent.api.repository.IdempotencyRepository;
import com.xjtu.iron.idempotent.api.spi.IdempotencyFailureClassifier;
import com.xjtu.iron.idempotent.api.spi.IdempotencyResultCodec;
import com.xjtu.iron.idempotent.core.DefaultIdempotencyExecutor;
import com.xjtu.iron.idempotent.core.DefaultIdempotencyFailureClassifier;
import com.xjtu.iron.idempotent.core.DefaultIdempotencyRepositoryRegistry;
import com.xjtu.iron.idempotent.core.IdempotencyDefaults;
import com.xjtu.iron.idempotent.core.IdempotencyOwnerTokenGenerator;
import com.xjtu.iron.idempotent.core.IdempotencyRepositoryRegistry;
import com.xjtu.iron.idempotent.core.UuidIdempotencyOwnerTokenGenerator;
import com.xjtu.iron.idempotent.core.observation.IdempotencyEventPublisher;
import com.xjtu.iron.idempotent.core.observation.IdempotencyMetrics;
import com.xjtu.iron.idempotent.provider.jdbc.JdbcIdempotencyRepository;
import com.xjtu.iron.idempotent.provider.redis.RedisIdempotencyRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
 * <p>Starter 负责“装配”，Core 不依赖 Spring。Redis/MySQL 连接仍由应用级
 * {@code spring.data.redis.*}/{@code spring.datasource.*} 提供。</p>
 */
@AutoConfiguration
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
        // 保守默认：普通业务异常标记为不可重试。
        return new DefaultIdempotencyFailureClassifier();
    }

    @Bean
    @ConditionalOnClass(ObjectMapper.class)
    @ConditionalOnMissingBean(IdempotencyResultCodec.class)
    public IdempotencyResultCodec idempotencyResultCodec(ObjectProvider<ObjectMapper> provider) {
        return new JacksonIdempotencyResultCodec(provider.getIfAvailable(ObjectMapper::new));
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

    /**
     * SHORT_TERM 默认 Repository。只在应用已有 StringRedisTemplate 时创建。
     */
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
     * DURABLE 默认 Repository。只在应用已有 DataSource 时创建。
     */
    @Bean(name = "jdbcIdempotencyRepository")
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnProperty(
            prefix = "xjtu.iron.idempotent.jdbc",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public IdempotencyRepository jdbcIdempotencyRepository(
            DataSource dataSource,
            IdempotencyProperties properties) {
        return new JdbcIdempotencyRepository(dataSource, properties.getJdbc().getTableName());
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
                .recordTtl(properties.getShortTermRecordTtl())
                .retryOnProcessingTimeout(properties.isRetryOnProcessingTimeout())
                .retryFailed(properties.isRetryFailed())
                .storeResult(properties.isStoreResult())
                .lockOptions(lock)
                .build();

        IdempotencyOptions durable = IdempotencyOptions.builder()
                .mode(IdempotencyMode.DURABLE)
                .processingTimeout(properties.getProcessingTimeout())
                .retryOnProcessingTimeout(properties.isRetryOnProcessingTimeout())
                .retryFailed(properties.isRetryFailed())
                .storeResult(properties.isStoreResult())
                .lockOptions(lock)
                .build();

        return new IdempotencyDefaults(properties.getDefaultMode(), shortTerm, durable);
    }

    @Bean
    @ConditionalOnMissingBean(IdempotencyExecutor.class)
    public IdempotencyExecutor idempotencyExecutor(
            IdempotencyRepositoryRegistry registry,
            IdempotencyDefaults defaults,
            IdempotencyOwnerTokenGenerator ownerGenerator,
            IdempotencyFailureClassifier failureClassifier,
            ObjectProvider<IdempotencyResultCodec> codec,
            ObjectProvider<DistributedLockClient> lockClient,
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
                eventPublisher.getIfAvailable(IdempotencyEventPublisher::noop),
                metrics.getIfAvailable(IdempotencyMetrics::noop),
                clock);
    }
}
