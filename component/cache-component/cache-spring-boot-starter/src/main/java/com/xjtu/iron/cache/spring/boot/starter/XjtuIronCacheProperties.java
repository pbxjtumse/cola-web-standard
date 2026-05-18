package com.xjtu.iron.cache.spring.boot.starter;

import com.xjtu.iron.cache.api.enums.CacheDegradePolicy;
import com.xjtu.iron.cache.api.enums.CacheNullPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "xjtu.iron.cache")
public class XjtuIronCacheProperties {

    private boolean enabled = true;

    private CaffeineProperties caffeine = new CaffeineProperties();

    private RedisProperties redis = new RedisProperties();

    private Map<String, CacheSpecProperties> specs = new HashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public CaffeineProperties getCaffeine() {
        return caffeine;
    }

    public void setCaffeine(CaffeineProperties caffeine) {
        this.caffeine = caffeine;
    }

    public RedisProperties getRedis() {
        return redis;
    }

    public void setRedis(RedisProperties redis) {
        this.redis = redis;
    }

    public Map<String, CacheSpecProperties> getSpecs() {
        return specs;
    }

    public void setSpecs(Map<String, CacheSpecProperties> specs) {
        this.specs = specs;
    }

    public static class CaffeineProperties {

        private boolean enabled = true;

        private long defaultMaximumSize = 10000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getDefaultMaximumSize() {
            return defaultMaximumSize;
        }

        public void setDefaultMaximumSize(long defaultMaximumSize) {
            this.defaultMaximumSize = defaultMaximumSize;
        }
    }

    public static class RedisProperties {

        private boolean enabled = true;

        private String keyPrefix = "";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }
    }

    public static class CacheSpecProperties {

        private boolean enableL1 = true;

        private boolean enableL2 = true;

        private Duration ttl = Duration.ofMinutes(10);

        private Duration nullValueTtl = Duration.ofSeconds(30);

        private Duration ttlJitter = Duration.ofSeconds(60);

        private boolean mutexLoad = true;

        private CacheNullPolicy nullPolicy = CacheNullPolicy.CACHE_NULL;

        private CacheDegradePolicy degradePolicy = CacheDegradePolicy.LOAD_SOURCE;

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

        public CacheDegradePolicy getDegradePolicy() {
            return degradePolicy;
        }

        public void setDegradePolicy(CacheDegradePolicy degradePolicy) {
            this.degradePolicy = degradePolicy;
        }
    }
}
