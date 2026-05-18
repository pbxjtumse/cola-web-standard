package com.xjtu.iron.cache.api;


import com.xjtu.iron.cache.api.enums.CacheConsistencyPolicy;
import com.xjtu.iron.cache.api.enums.CacheDegradePolicy;
import com.xjtu.iron.cache.api.enums.CacheNullPolicy;

import java.time.Duration;

public class CacheSpec {

    private String cacheName;

    private boolean enableL1 = true;

    private boolean enableL2 = true;

    private Duration ttl = Duration.ofMinutes(10);

    private Duration nullValueTtl = Duration.ofSeconds(30);

    private Duration ttlJitter = Duration.ofSeconds(60);

    private boolean mutexLoad = true;

    private CacheNullPolicy nullPolicy = CacheNullPolicy.CACHE_NULL;

    private CacheConsistencyPolicy consistencyPolicy = CacheConsistencyPolicy.EVENTUAL;

    private CacheDegradePolicy degradePolicy = CacheDegradePolicy.LOAD_SOURCE;

    public static CacheSpec defaults(String cacheName) {
        CacheSpec spec = new CacheSpec();
        spec.setCacheName(cacheName);
        return spec;
    }

    public String getCacheName() {
        return cacheName;
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    public boolean isEnableL1() {
        return enableL1;
    }

    public void setEnableL1(boolean enableL1) {
        this.enableL1 = enableL1;
    }

    public boolean isEnableL2() {
        return enableL2;
    }

    public void setEnableL2(boolean enableL2) {
        this.enableL2 = enableL2;
    }

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }

    public Duration getNullValueTtl() {
        return nullValueTtl;
    }

    public void setNullValueTtl(Duration nullValueTtl) {
        this.nullValueTtl = nullValueTtl;
    }

    public Duration getTtlJitter() {
        return ttlJitter;
    }

    public void setTtlJitter(Duration ttlJitter) {
        this.ttlJitter = ttlJitter;
    }

    public boolean isMutexLoad() {
        return mutexLoad;
    }

    public void setMutexLoad(boolean mutexLoad) {
        this.mutexLoad = mutexLoad;
    }

    public CacheNullPolicy getNullPolicy() {
        return nullPolicy;
    }

    public void setNullPolicy(CacheNullPolicy nullPolicy) {
        this.nullPolicy = nullPolicy;
    }

    public CacheConsistencyPolicy getConsistencyPolicy() {
        return consistencyPolicy;
    }

    public void setConsistencyPolicy(CacheConsistencyPolicy consistencyPolicy) {
        this.consistencyPolicy = consistencyPolicy;
    }

    public CacheDegradePolicy getDegradePolicy() {
        return degradePolicy;
    }

    public void setDegradePolicy(CacheDegradePolicy degradePolicy) {
        this.degradePolicy = degradePolicy;
    }
}
