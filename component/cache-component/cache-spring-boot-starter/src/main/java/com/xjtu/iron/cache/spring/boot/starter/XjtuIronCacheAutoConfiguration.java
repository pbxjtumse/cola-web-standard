package com.xjtu.iron.cache.spring.boot.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xjtu.iron.cache.api.CacheClient;
import com.xjtu.iron.cache.core.*;
import com.xjtu.iron.cache.core.impl.*;
import com.xjtu.iron.cache.integrations.observability.MicrometerCacheMetricsRecorder;
import com.xjtu.iron.cache.provider.caffeine.CaffeineCacheManager;
import com.xjtu.iron.cache.provider.caffeine.CaffeineCacheProvider;
import com.xjtu.iron.cache.provider.composite.CompositeCacheProvider;
import com.xjtu.iron.cache.provider.redis.JacksonRedisCacheSerializer;
import com.xjtu.iron.cache.provider.redis.RedisBinaryClient;
import com.xjtu.iron.cache.provider.redis.RedisCacheProvider;
import com.xjtu.iron.cache.provider.redis.RedisCacheSerializer;
import com.xjtu.iron.cache.provider.redis.SpringDataRedisBinaryClient;
import com.xjtu.iron.cache.provider.redis.invalidation.RedisCacheInvalidationSubscriber;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.UUID;

/**
 * 缓存组件 Spring Boot 自动装配类。
 *
 * <p>业务系统引入 cache-spring-boot-starter 后，Spring Boot 会通过
 * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports 加载本类。</p>
 */
@AutoConfiguration(after = RedisAutoConfiguration.class)
@EnableConfigurationProperties(XjtuIronCacheProperties.class)
@ConditionalOnProperty(prefix = "xjtu.iron.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class XjtuIronCacheAutoConfiguration {

    /** 创建基于配置文件的缓存策略解析器。 */
    @Bean
    @ConditionalOnMissingBean
    public CacheSpecResolver cacheSpecResolver(XjtuIronCacheProperties properties) {
        return new PropertiesCacheSpecResolver(properties);
    }

    /** 创建 TTL 解析器。 */
    @Bean
    @ConditionalOnMissingBean
    public CacheTtlResolver cacheTtlResolver() {
        return new CacheTtlResolver();
    }

    /** 创建本地互斥加载保护器。 */
    @Bean
    @ConditionalOnMissingBean
    public CacheLoadGuard cacheLoadGuard() {
        return new LocalMutexCacheLoadGuard();
    }

    /** 如果存在 MeterRegistry，则使用 Micrometer 指标记录器。 */
    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnMissingBean(CacheMetricsRecorder.class)
    public CacheMetricsRecorder micrometerCacheMetricsRecorder(MeterRegistry meterRegistry) {
        return new MicrometerCacheMetricsRecorder(meterRegistry);
    }

    /** 如果没有指标系统，则使用空实现，保证组件正常启动。 */
    @Bean
    @ConditionalOnMissingBean(CacheMetricsRecorder.class)
    public CacheMetricsRecorder noopCacheMetricsRecorder() {
        return new NoopCacheMetricsRecorder();
    }

    /** 创建 Caffeine Cache 管理器。 */
    @Bean("ironCaffeineCacheManager")
    @ConditionalOnProperty(prefix = "xjtu.iron.cache.caffeine", name = "enabled", havingValue = "true", matchIfMissing = true)
    public CaffeineCacheManager caffeineCacheManager(XjtuIronCacheProperties properties) {
        return new CaffeineCacheManager(properties.getCaffeine().getDefaultMaximumSize());
    }

    /** 创建 Caffeine Provider。 */
    @Bean("ironCaffeineCacheProvider")
    @ConditionalOnBean(name = "ironCaffeineCacheManager")
    public CaffeineCacheProvider caffeineCacheProvider(
            @Qualifier("ironCaffeineCacheManager") CaffeineCacheManager caffeineCacheManager
    ) {
        return new CaffeineCacheProvider(caffeineCacheManager);
    }

    /** 创建缓存组件专用 RedisTemplate。 */
    @Bean("ironCacheRedisTemplate")
    @ConditionalOnBean(RedisConnectionFactory.class)
    @ConditionalOnMissingBean(name = "ironCacheRedisTemplate")
    public RedisTemplate<String, byte[]> ironCacheRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, byte[]> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(RedisSerializer.byteArray());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(RedisSerializer.byteArray());
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    /** 创建 Redis value 序列化器。 */
    @Bean
    @ConditionalOnMissingBean
    public RedisCacheSerializer redisCacheSerializer() {
        return new JacksonRedisCacheSerializer();
    }

    /** 创建 Redis 二进制客户端。 */
    @Bean
    @ConditionalOnBean(name = "ironCacheRedisTemplate")
    @ConditionalOnMissingBean
    public RedisBinaryClient redisBinaryClient(@Qualifier("ironCacheRedisTemplate") RedisTemplate<String, byte[]> redisTemplate) {
        return new SpringDataRedisBinaryClient(redisTemplate);
    }

    /** 创建 Redis Provider。 */
    @Bean("ironRedisCacheProvider")
    @ConditionalOnBean(RedisBinaryClient.class)
    @ConditionalOnProperty(prefix = "xjtu.iron.cache.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    public CacheProvider redisCacheProvider(RedisBinaryClient redisBinaryClient, RedisCacheSerializer redisCacheSerializer,
                                            XjtuIronCacheProperties properties) {
        return new RedisCacheProvider(redisBinaryClient, redisCacheSerializer, properties.getRedis().getKeyPrefix());
    }

    /** 创建 L1 + L2 组合 Provider。 */
    @Bean("ironCompositeCacheProvider")
    @Primary
    @ConditionalOnBean(name = {"ironCaffeineCacheProvider", "ironRedisCacheProvider"})
    public CacheProvider compositeCacheProvider(
            @Qualifier("ironCaffeineCacheProvider") CacheProvider caffeineProvider,
            @Qualifier("ironRedisCacheProvider") CacheProvider redisProvider,
            CacheInvalidationPublisher invalidationPublisher
    ) {
        return new CompositeCacheProvider(
                caffeineProvider,
                redisProvider,
                invalidationPublisher
        );
    }

    /** 创建业务最终注入的 CacheClient。 */
    @Bean
    @ConditionalOnBean(name = "ironCompositeCacheProvider")
    @ConditionalOnMissingBean
    public CacheClient cacheClient(@Qualifier("ironCompositeCacheProvider") CacheProvider cacheProvider,
                                   CacheSpecResolver cacheSpecResolver,
                                   CacheTtlResolver cacheTtlResolver,
                                   CacheLoadGuard cacheLoadGuard,
                                   CacheMetricsRecorder cacheMetricsRecorder) {
        return new DefaultCacheClient(cacheProvider, cacheSpecResolver, cacheTtlResolver, cacheLoadGuard, cacheMetricsRecorder);
    }

    @Bean("ironCacheInstanceId")
    @ConditionalOnMissingBean(name = "ironCacheInstanceId")
    public String ironCacheInstanceId(
            XjtuIronCacheProperties properties,
            Environment environment
    ) {
        String configuredInstanceId = properties.getInvalidation().getInstanceId();

        if (configuredInstanceId != null && !configuredInstanceId.isBlank()) {
            return configuredInstanceId;
        }

        String appName = environment.getProperty("spring.application.name", "unknown-app");

        return appName + "-" + UUID.randomUUID();
    }

    @Bean("ironCacheStringRedisTemplate")
    @ConditionalOnBean(RedisConnectionFactory.class)
    @ConditionalOnMissingBean(name = "ironCacheStringRedisTemplate")
    public StringRedisTemplate ironCacheStringRedisTemplate(
            RedisConnectionFactory redisConnectionFactory
    ) {
        return new StringRedisTemplate(redisConnectionFactory);
    }

    @Bean("ironCacheObjectMapper")
    @ConditionalOnMissingBean(name = "ironCacheObjectMapper")
    public ObjectMapper ironCacheObjectMapper(ObjectProvider<ObjectMapper> objectMapperProvider) {
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable();

        if (objectMapper != null) {
            return objectMapper;
        }

        return new ObjectMapper();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "xjtu.iron.cache.invalidation",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    @ConditionalOnBean(name = "ironCacheStringRedisTemplate")
    public CacheInvalidationPublisher redisCacheInvalidationPublisher(
            @Qualifier("ironCacheStringRedisTemplate") StringRedisTemplate stringRedisTemplate,
            @Qualifier("ironCacheObjectMapper") ObjectMapper objectMapper,
            @Qualifier("ironCacheInstanceId") String instanceId,
            XjtuIronCacheProperties properties
    ) {
        return new RedisCacheInvalidationPublisher(
                stringRedisTemplate,
                objectMapper,
                properties.getInvalidation().getChannel(),
                instanceId
        );
    }

    @Bean
    @ConditionalOnMissingBean(CacheInvalidationPublisher.class)
    public CacheInvalidationPublisher noopCacheInvalidationPublisher() {
        return new NoopCacheInvalidationPublisher();
    }


    @Bean
    @ConditionalOnProperty(
            prefix = "xjtu.iron.cache.invalidation",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    @ConditionalOnBean(LocalCacheInvalidator.class)
    public RedisCacheInvalidationSubscriber redisCacheInvalidationSubscriber(
            @Qualifier("ironCacheObjectMapper") ObjectMapper objectMapper,
            LocalCacheInvalidator localCacheInvalidator,
            @Qualifier("ironCacheInstanceId") String instanceId
    ) {
        return new RedisCacheInvalidationSubscriber(
                objectMapper,
                localCacheInvalidator,
                instanceId
        );
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "xjtu.iron.cache.invalidation",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    @ConditionalOnBean(RedisCacheInvalidationSubscriber.class)
    public RedisMessageListenerContainer ironCacheRedisMessageListenerContainer(
            RedisConnectionFactory redisConnectionFactory,
            RedisCacheInvalidationSubscriber subscriber,
            XjtuIronCacheProperties properties
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(
                subscriber,
                new ChannelTopic(properties.getInvalidation().getChannel())
        );
        return container;
    }


}
