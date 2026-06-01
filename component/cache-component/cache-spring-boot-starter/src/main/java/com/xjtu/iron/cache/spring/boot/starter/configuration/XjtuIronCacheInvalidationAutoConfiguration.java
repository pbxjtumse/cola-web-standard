package com.xjtu.iron.cache.spring.boot.starter.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xjtu.iron.cache.core.CacheInvalidationPublisher;
import com.xjtu.iron.cache.core.LocalCacheInvalidator;
import com.xjtu.iron.cache.core.impl.NoopCacheInvalidationPublisher;
import com.xjtu.iron.cache.provider.redis.invalidation.RedisCacheInvalidationPublisher;
import com.xjtu.iron.cache.provider.redis.invalidation.RedisCacheInvalidationSubscriber;
import com.xjtu.iron.cache.spring.boot.starter.XjtuIronCacheProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.UUID;

/**
 * 本地缓存失效通知自动装配。
 *
 * <p>二期使用 Redis Pub/Sub 实现多实例 L1 本地缓存失效通知。</p>
 *
 * <p>职责：</p>
 *
 * <pre>
 * 1. 生成当前实例 ID
 * 2. 创建失效事件发布器
 * 3. 创建失效事件订阅器
 * 4. 创建 Redis 消息监听容器
 * 5. 在未开启通知时提供 Noop 发布器
 * </pre>
 */
@AutoConfiguration(after = {
        XjtuIronCacheRedisAutoConfiguration.class,
        XjtuIronCacheCaffeineAutoConfiguration.class
})
@ConditionalOnProperty(
        prefix = "xjtu.iron.cache",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class XjtuIronCacheInvalidationAutoConfiguration {

    /**
     * 创建当前应用实例 ID。
     *
     * <p>实例 ID 用于区分同一个应用的不同 JVM 实例，避免订阅端重复处理自己发布的消息。</p>
     *
     * @param properties 缓存组件配置属性
     * @param environment Spring 环境
     * @return 当前实例 ID
     */
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

    /**
     * 创建缓存失效事件使用的 ObjectMapper。
     *
     * <p>优先复用业务系统已有 ObjectMapper；如果不存在，则创建默认 ObjectMapper。</p>
     *
     * @param objectMapperProvider Spring 中已有的 ObjectMapper Provider
     * @return ObjectMapper
     */
    @Bean("ironCacheObjectMapper")
    @ConditionalOnMissingBean(name = "ironCacheObjectMapper")
    public ObjectMapper ironCacheObjectMapper(ObjectProvider<ObjectMapper> objectMapperProvider) {
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable();
        return objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * 创建 Redis Pub/Sub 缓存失效事件发布器。
     *
     * @param stringRedisTemplate Redis 字符串模板
     * @param objectMapper JSON 序列化器
     * @param instanceId 当前实例 ID
     * @param properties 缓存组件配置属性
     * @return 缓存失效事件发布器
     */
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

    /**
     * 创建空实现缓存失效事件发布器。
     *
     * <p>当本地缓存失效通知未开启，或者 Redis Pub/Sub 条件不满足时使用。</p>
     *
     * @return 空实现失效事件发布器
     */
    @Bean
    @ConditionalOnMissingBean(CacheInvalidationPublisher.class)
    public CacheInvalidationPublisher noopCacheInvalidationPublisher() {
        return new NoopCacheInvalidationPublisher();
    }

    /**
     * 创建 Redis Pub/Sub 缓存失效事件订阅器。
     *
     * @param objectMapper JSON 反序列化器
     * @param localCacheInvalidator 本地缓存失效器
     * @param instanceId 当前实例 ID
     * @return Redis 缓存失效事件订阅器
     */
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

    /**
     * 创建 Redis 消息监听容器。
     *
     * <p>该容器负责订阅 Redis channel，并把消息回调给 RedisCacheInvalidationSubscriber。</p>
     *
     * @param redisConnectionFactory Redis 连接工厂
     * @param subscriber Redis 缓存失效事件订阅器
     * @param properties 缓存组件配置属性
     * @return Redis 消息监听容器
     */
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
