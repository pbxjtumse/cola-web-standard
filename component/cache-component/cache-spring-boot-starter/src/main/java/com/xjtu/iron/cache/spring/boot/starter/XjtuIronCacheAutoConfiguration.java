package com.xjtu.iron.cache.spring.boot.starter;

import com.xjtu.iron.cache.api.CacheClient;
import com.xjtu.iron.cache.core.CacheLoadGuard;
import com.xjtu.iron.cache.core.CacheMetricsRecorder;
import com.xjtu.iron.cache.core.CacheProvider;
import com.xjtu.iron.cache.core.CacheSpecResolver;
import com.xjtu.iron.cache.core.CacheTtlResolver;
import com.xjtu.iron.cache.core.DefaultCacheClient;
import com.xjtu.iron.cache.core.LocalMutexCacheLoadGuard;
import com.xjtu.iron.cache.core.NoopCacheMetricsRecorder;
import com.xjtu.iron.cache.integrations.observability.MicrometerCacheMetricsRecorder;
import com.xjtu.iron.cache.provider.caffeine.CaffeineCacheManager;
import com.xjtu.iron.cache.provider.caffeine.CaffeineCacheProvider;
import com.xjtu.iron.cache.provider.composite.CompositeCacheProvider;
import com.xjtu.iron.cache.provider.redis.JacksonRedisCacheSerializer;
import com.xjtu.iron.cache.provider.redis.RedisBinaryClient;
import com.xjtu.iron.cache.provider.redis.RedisCacheProvider;
import com.xjtu.iron.cache.provider.redis.RedisCacheSerializer;
import com.xjtu.iron.cache.provider.redis.SpringDataRedisBinaryClient;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

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
    public CacheProvider caffeineCacheProvider(@Qualifier("ironCaffeineCacheManager") CaffeineCacheManager caffeineCacheManager) {
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
    public CacheProvider compositeCacheProvider(@Qualifier("ironCaffeineCacheProvider") CacheProvider caffeineProvider,
                                                @Qualifier("ironRedisCacheProvider") CacheProvider redisProvider) {
        return new CompositeCacheProvider(caffeineProvider, redisProvider);
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
}
