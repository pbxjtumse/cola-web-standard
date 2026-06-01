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
    @ConditionalOnMissingBean(CacheInvalidationPublisher.class)
    public CacheInvalidationPublisher noopCacheInvalidationPublisher() {
        return new NoopCacheInvalidationPublisher();
    }







}
