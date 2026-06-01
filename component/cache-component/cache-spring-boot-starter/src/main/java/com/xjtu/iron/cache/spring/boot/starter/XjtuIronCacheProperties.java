package com.xjtu.iron.cache.spring.boot.starter;

import com.xjtu.iron.cache.api.enums.CacheDegradePolicy;
import com.xjtu.iron.cache.api.enums.CacheNullPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * XJTU Iron Cache Spring Boot 配置属性。
 *
 * <p>对应配置前缀：xjtu.iron.cache。</p>
 */
@ConfigurationProperties(prefix = "xjtu.iron.cache")
public class XjtuIronCacheProperties {

    /** 是否启用缓存组件自动装配。 */
    private boolean enabled = true;

    /** Caffeine L1 本地缓存配置。 */
    private CaffeineProperties caffeine = new CaffeineProperties();

    /** Redis L2 分布式缓存配置。 */
    private RedisProperties redis = new RedisProperties();

    /** cacheName 到缓存策略配置的映射。 */
    private Map<String, CacheSpecProperties> specs = new HashMap<>();

    /** 返回是否启用缓存组件。 */
    public boolean isEnabled() { return enabled; }

    /** 设置是否启用缓存组件。 */
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /** 返回 Caffeine 配置。 */
    public CaffeineProperties getCaffeine() { return caffeine; }

    /** 设置 Caffeine 配置。 */
    public void setCaffeine(CaffeineProperties caffeine) { this.caffeine = caffeine; }

    /** 返回 Redis 配置。 */
    public RedisProperties getRedis() { return redis; }

    /** 设置 Redis 配置。 */
    public void setRedis(RedisProperties redis) { this.redis = redis; }

    /** 返回缓存策略配置映射。 */
    public Map<String, CacheSpecProperties> getSpecs() { return specs; }

    /** 设置缓存策略配置映射。 */
    public void setSpecs(Map<String, CacheSpecProperties> specs) { this.specs = specs; }

    /** Caffeine 本地缓存属性。 */
    public static class CaffeineProperties {
        /** 是否启用 Caffeine。 */
        private boolean enabled = true;
        /** 每个 cacheName 默认最大本地缓存条目数。 */
        private long defaultMaximumSize = 10000;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public long getDefaultMaximumSize() { return defaultMaximumSize; }
        public void setDefaultMaximumSize(long defaultMaximumSize) { this.defaultMaximumSize = defaultMaximumSize; }
    }

    /** Redis 分布式缓存属性。 */
    public static class RedisProperties {
        /** 是否启用 Redis。 */
        private boolean enabled = true;
        /** Redis key 全局前缀。 */
        private String keyPrefix = "";
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getKeyPrefix() { return keyPrefix; }
        public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }
    }

    /** 单个 cacheName 的缓存策略属性。 */
    public static class CacheSpecProperties {
        /** 是否启用 L1 Caffeine。 */
        private boolean enableL1 = true;
        /** 是否启用 L2 Redis。 */
        private boolean enableL2 = true;
        /** 正常值 TTL。 */
        private Duration ttl = Duration.ofMinutes(10);
        /** 空值 TTL。 */
        private Duration nullValueTtl = Duration.ofSeconds(30);
        /** TTL 抖动范围。 */
        private Duration ttlJitter = Duration.ofSeconds(60);
        /** 是否启用本地互斥加载。 */
        private boolean mutexLoad = true;
        /** 空值缓存策略。 */
        private CacheNullPolicy nullPolicy = CacheNullPolicy.CACHE_NULL;
        /** 缓存异常降级策略。 */
        private CacheDegradePolicy degradePolicy = CacheDegradePolicy.LOAD_SOURCE;
        public boolean isEnableL1() { return enableL1; }
        public void setEnableL1(boolean enableL1) { this.enableL1 = enableL1; }
        public boolean isEnableL2() { return enableL2; }
        public void setEnableL2(boolean enableL2) { this.enableL2 = enableL2; }
        public Duration getTtl() { return ttl; }
        public void setTtl(Duration ttl) { this.ttl = ttl; }
        public Duration getNullValueTtl() { return nullValueTtl; }
        public void setNullValueTtl(Duration nullValueTtl) { this.nullValueTtl = nullValueTtl; }
        public Duration getTtlJitter() { return ttlJitter; }
        public void setTtlJitter(Duration ttlJitter) { this.ttlJitter = ttlJitter; }
        public boolean isMutexLoad() { return mutexLoad; }
        public void setMutexLoad(boolean mutexLoad) { this.mutexLoad = mutexLoad; }
        public CacheNullPolicy getNullPolicy() { return nullPolicy; }
        public void setNullPolicy(CacheNullPolicy nullPolicy) { this.nullPolicy = nullPolicy; }
        public CacheDegradePolicy getDegradePolicy() { return degradePolicy; }
        public void setDegradePolicy(CacheDegradePolicy degradePolicy) { this.degradePolicy = degradePolicy; }
    }
}
