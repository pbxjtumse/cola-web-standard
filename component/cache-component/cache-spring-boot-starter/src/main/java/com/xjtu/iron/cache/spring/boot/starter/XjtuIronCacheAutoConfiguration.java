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
//import io.micrometer.core.instrument.MeterRegistry;
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
import org.springframework.data.redis.serializer.ByteArrayRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@AutoConfiguration(after = RedisAutoConfiguration.class)
@EnableConfigurationProperties(XjtuIronCacheProperties.class)
@ConditionalOnProperty(prefix = "xjtu.iron.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class XjtuIronCacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CacheSpecResolver cacheSpecResolver(XjtuIronCacheProperties properties) {
        return new PropertiesCacheSpecResolver(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheTtlResolver cacheTtlResolver() {
        return new CacheTtlResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheLoadGuard cacheLoadGuard() {
        return new LocalMutexCacheLoadGuard();
    }

//    @Bean
//    @ConditionalOnBean(MeterRegistry.class)
//    @ConditionalOnMissingBean(CacheMetricsRecorder.class)
//    public CacheMetricsRecorder micrometerCacheMetricsRecorder(MeterRegistry meterRegistry) {
//        return new MicrometerCacheMetricsRecorder(meterRegistry);
//    }

    @Bean
    @ConditionalOnMissingBean(CacheMetricsRecorder.class)
    public CacheMetricsRecorder noopCacheMetricsRecorder() {
        return new NoopCacheMetricsRecorder();
    }

    @Bean("ironCaffeineCacheManager")
    @ConditionalOnProperty(
            prefix = "xjtu.iron.cache.caffeine",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public CaffeineCacheManager caffeineCacheManager(XjtuIronCacheProperties properties) {
        return new CaffeineCacheManager(properties.getCaffeine().getDefaultMaximumSize());
    }

    @Bean("ironCaffeineCacheProvider")
    @ConditionalOnBean(name = "ironCaffeineCacheManager")
    public CacheProvider caffeineCacheProvider(
            @Qualifier("ironCaffeineCacheManager") CaffeineCacheManager caffeineCacheManager
    ) {
        return new CaffeineCacheProvider(caffeineCacheManager);
    }

    @Bean("ironCacheRedisTemplate")
    @ConditionalOnBean(RedisConnectionFactory.class)
    @ConditionalOnMissingBean(name = "ironCacheRedisTemplate")
    public RedisTemplate<String, byte[]> ironCacheRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, byte[]> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new ByteArrayRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new ByteArrayRedisSerializer());
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisCacheSerializer redisCacheSerializer() {
        return new JacksonRedisCacheSerializer();
    }

    @Bean
    @ConditionalOnBean(name = "ironCacheRedisTemplate")
    @ConditionalOnMissingBean
    public RedisBinaryClient redisBinaryClient(
            @Qualifier("ironCacheRedisTemplate") RedisTemplate<String, byte[]> redisTemplate
    ) {
        return new SpringDataRedisBinaryClient(redisTemplate);
    }

    @Bean("ironRedisCacheProvider")
    @ConditionalOnBean(RedisBinaryClient.class)
    @ConditionalOnProperty(
            prefix = "xjtu.iron.cache.redis",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public CacheProvider redisCacheProvider(
            RedisBinaryClient redisBinaryClient,
            RedisCacheSerializer redisCacheSerializer,
            XjtuIronCacheProperties properties
    ) {
        return new RedisCacheProvider(
                redisBinaryClient,
                redisCacheSerializer,
                properties.getRedis().getKeyPrefix()
        );
    }

    @Bean("ironCompositeCacheProvider")
    @Primary
    @ConditionalOnBean(name = {"ironCaffeineCacheProvider", "ironRedisCacheProvider"})
    public CacheProvider compositeCacheProvider(
            @Qualifier("ironCaffeineCacheProvider") CacheProvider caffeineProvider,
            @Qualifier("ironRedisCacheProvider") CacheProvider redisProvider
    ) {
        return new CompositeCacheProvider(caffeineProvider, redisProvider);
    }

    @Bean
    @ConditionalOnBean(name = "ironCompositeCacheProvider")
    @ConditionalOnMissingBean
    public CacheClient cacheClient(
            @Qualifier("ironCompositeCacheProvider") CacheProvider cacheProvider,
            CacheSpecResolver cacheSpecResolver,
            CacheTtlResolver cacheTtlResolver,
            CacheLoadGuard cacheLoadGuard,
            CacheMetricsRecorder cacheMetricsRecorder
    ) {
        return new DefaultCacheClient(
                cacheProvider,
                cacheSpecResolver,
                cacheTtlResolver,
                cacheLoadGuard,
                cacheMetricsRecorder
        );
    }
}
