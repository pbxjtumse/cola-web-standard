package com.xjtu.iron.cache.spring.boot.starter;

import com.xjtu.iron.cache.api.CacheKey;
import com.xjtu.iron.cache.api.CacheSpec;
import com.xjtu.iron.cache.core.CacheSpecResolver;

/**
 * 基于 Spring Boot 配置文件的缓存策略解析器。
 *
 * <p>它负责把 {@link XjtuIronCacheProperties} 中的配置转换成运行时使用的
 * {@link CacheSpec}。</p>
 *
 * <p>例如配置项：</p>
 *
 * <pre>
 * xjtu.iron.cache.specs.campaignRule.ttl=5m
 * xjtu.iron.cache.specs.campaignRule.enable-l1=true
 * </pre>
 *
 * <p>会被解析成 cacheName 为 campaignRule 的 CacheSpec。</p>
 */
public class PropertiesCacheSpecResolver implements CacheSpecResolver {

    /**
     * Spring Boot 绑定后的缓存组件配置属性。
     */
    private final XjtuIronCacheProperties properties;

    /**
     * 创建缓存策略解析器。
     *
     * @param properties 缓存组件配置属性
     */
    public PropertiesCacheSpecResolver(XjtuIronCacheProperties properties) {
        this.properties = properties;
    }

    /**
     * 根据 CacheKey 解析缓存策略。
     *
     * <p>实际使用的是 key 中的 cacheName。</p>
     *
     * @param key 缓存 key
     * @return 缓存策略
     */
    @Override
    public CacheSpec resolve(CacheKey key) {
        return resolve(key.cacheName());
    }

    /**
     * 根据 cacheName 解析缓存策略。
     *
     * <p>如果配置文件中没有该 cacheName 的配置，则返回默认策略。</p>
     *
     * @param cacheName 缓存名称
     * @return 缓存策略
     */
    @Override
    public CacheSpec resolve(String cacheName) {
        XjtuIronCacheProperties.CacheSpecProperties prop = properties.getSpecs().get(cacheName);
        CacheSpec spec = CacheSpec.defaults(cacheName);

        if (prop == null) {
            return spec;
        }

        spec.setEnableL1(prop.isEnableL1());
        spec.setEnableL2(prop.isEnableL2());
        spec.setTtl(prop.getTtl());
        spec.setNullValueTtl(prop.getNullValueTtl());
        spec.setTtlJitter(prop.getTtlJitter());
        spec.setMutexLoad(prop.isMutexLoad());
        spec.setNullPolicy(prop.getNullPolicy());
        spec.setDegradePolicy(prop.getDegradePolicy());
        return spec;
    }
}
