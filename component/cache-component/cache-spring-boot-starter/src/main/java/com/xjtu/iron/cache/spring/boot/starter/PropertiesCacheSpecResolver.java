package com.xjtu.iron.cache.spring.boot.starter;


import com.xjtu.iron.cache.api.CacheKey;
import com.xjtu.iron.cache.api.CacheSpec;
import com.xjtu.iron.cache.core.CacheSpecResolver;

public class PropertiesCacheSpecResolver implements CacheSpecResolver {

    private final XjtuIronCacheProperties properties;

    public PropertiesCacheSpecResolver(XjtuIronCacheProperties properties) {
        this.properties = properties;
    }

    @Override
    public CacheSpec resolve(CacheKey key) {
        return resolve(key.cacheName());
    }

    @Override
    public CacheSpec resolve(String cacheName) {
        XjtuIronCacheProperties.CacheSpecProperties prop =
                properties.getSpecs().get(cacheName);

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
