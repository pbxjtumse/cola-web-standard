package com.xjtu.iron.cache.spring.boot.starter;

import com.xjtu.iron.cache.api.CacheKey;
import com.xjtu.iron.cache.api.CacheSpec;
import com.xjtu.iron.cache.core.CacheSpecResolver;

/**
 * 基于 Spring Boot 配置属性的缓存策略解析器。
 */
public class PropertiesCacheSpecResolver implements CacheSpecResolver {

    /** starter 绑定的 xjtu.iron.cache 配置属性。 */
    private final XjtuIronCacheProperties properties;

    /** 创建解析器。 */
    public PropertiesCacheSpecResolver(XjtuIronCacheProperties properties) {
        this.properties = properties;
    }

    /** 根据 CacheKey 解析策略。 */
    @Override
    public CacheSpec resolve(CacheKey key) {
        return resolve(key.cacheName());
    }

    /** 根据 cacheName 从配置中解析策略；没有配置时返回默认策略。 */
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
