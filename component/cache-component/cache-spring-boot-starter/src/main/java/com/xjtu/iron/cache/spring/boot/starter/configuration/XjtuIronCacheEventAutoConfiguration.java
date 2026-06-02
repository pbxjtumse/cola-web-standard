package com.xjtu.iron.cache.spring.boot.starter.configuration;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.xjtu.iron.cache.core.event.CacheEventHandler;
import com.xjtu.iron.cache.core.event.CacheEventPublisher;
import com.xjtu.iron.cache.core.event.DefaultCacheEventHandler;
import com.xjtu.iron.cache.core.event.NoopCacheEventPublisher;
import com.xjtu.iron.cache.core.invalidate.LocalCacheInvalidator;
import com.xjtu.iron.cache.provider.redis.event.JacksonRedisCacheEventSerializer;
import com.xjtu.iron.cache.provider.redis.event.RedisCacheEventPublisher;
import com.xjtu.iron.cache.provider.redis.event.RedisCacheEventSerializer;
import com.xjtu.iron.cache.provider.redis.event.RedisCacheEventSubscriber;
import com.xjtu.iron.cache.spring.boot.starter.properties.XjtuIronCacheProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 缓存事件自动装配。
 *
 * <p>二期能力：</p>
 *
 * <pre>
 * Redis Pub/Sub 本地缓存失效通知。
 * </pre>
 */
@AutoConfiguration
public class XjtuIronCacheEventAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(CacheEventPublisher.class)
    public CacheEventPublisher noopCacheEventPublisher() {
        return new NoopCacheEventPublisher();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "xjtu.iron.cache.event",
            name = "enabled",
            havingValue = "true"
    )
    @ConditionalOnMissingBean
    public RedisCacheEventSerializer redisCacheEventSerializer(ObjectMapper objectMapper) {
        return new JacksonRedisCacheEventSerializer(objectMapper);
    }

    @Bean
    @Primary
    @ConditionalOnProperty(
            prefix = "xjtu.iron.cache.event",
            name = "enabled",
            havingValue = "true"
    )
    @ConditionalOnBean(RedisCacheEventSerializer.class)
    public CacheEventPublisher redisCacheEventPublisher(
            StringRedisTemplate stringRedisTemplate,
            RedisCacheEventSerializer serializer,
            XjtuIronCacheProperties properties
    ) {
        return new RedisCacheEventPublisher(
                stringRedisTemplate,
                serializer,
                properties.getEvent().getChannel()
        );
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "xjtu.iron.cache.event",
            name = "enabled",
            havingValue = "true"
    )
    @ConditionalOnBean(LocalCacheInvalidator.class)
    public CacheEventHandler cacheEventHandler(
            LocalCacheInvalidator localCacheInvalidator,
            XjtuIronCacheProperties properties
    ) {
        return new DefaultCacheEventHandler(
                localCacheInvalidator,
                properties.getApplication().getInstanceId()
        );
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "xjtu.iron.cache.event",
            name = "enabled",
            havingValue = "true"
    )
    @ConditionalOnBean({RedisCacheEventSerializer.class, CacheEventHandler.class})
    public RedisCacheEventSubscriber redisCacheEventSubscriber(
            RedisCacheEventSerializer serializer,
            CacheEventHandler eventHandler
    ) {
        return new RedisCacheEventSubscriber(serializer, eventHandler);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "xjtu.iron.cache.event",
            name = "enabled",
            havingValue = "true"
    )
    @ConditionalOnBean({RedisConnectionFactory.class, RedisCacheEventSubscriber.class})
    public RedisMessageListenerContainer redisCacheEventListenerContainer(
            RedisConnectionFactory redisConnectionFactory,
            RedisCacheEventSubscriber subscriber,
            XjtuIronCacheProperties properties
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(
                subscriber,
                new ChannelTopic(properties.getEvent().getChannel())
        );
        return container;
    }
}
