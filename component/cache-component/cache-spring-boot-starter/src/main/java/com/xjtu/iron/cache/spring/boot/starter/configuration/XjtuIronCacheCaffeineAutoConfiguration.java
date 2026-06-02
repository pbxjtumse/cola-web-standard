package com.xjtu.iron.cache.spring.boot.starter.configuration;

import com.xjtu.iron.cache.provider.caffeine.CaffeineCacheManager;
import com.xjtu.iron.cache.provider.caffeine.CaffeineCacheProvider;
import com.xjtu.iron.cache.spring.boot.starter.properties.XjtuIronCacheProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Caffeine L1 本地缓存自动装配。
 *
 * <p>负责创建：</p>
 *
 * <pre>
 * CaffeineCacheManager
 * CaffeineCacheProvider
 * </pre>
 *
 * <p>CaffeineCacheProvider 是一期 L1 本地缓存实现。</p>
 */
@AutoConfiguration(after = XjtuIronCacheAutoConfiguration.class)
@ConditionalOnProperty(
        prefix = "xjtu.iron.cache",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class XjtuIronCacheCaffeineAutoConfiguration {

    /**
     * 创建 Caffeine 缓存管理器。
     *
     * @param properties 缓存组件配置属性
     * @return Caffeine 缓存管理器
     */
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

    /**
     * 创建 Caffeine Provider。
     *
     * @param caffeineCacheManager Caffeine 缓存管理器
     * @return Caffeine Provider
     */
    @Bean("ironCaffeineCacheProvider")
    @ConditionalOnBean(name = "ironCaffeineCacheManager")
    public CaffeineCacheProvider caffeineCacheProvider(
            @Qualifier("ironCaffeineCacheManager") CaffeineCacheManager caffeineCacheManager
    ) {
        return new CaffeineCacheProvider(caffeineCacheManager);
    }
}
