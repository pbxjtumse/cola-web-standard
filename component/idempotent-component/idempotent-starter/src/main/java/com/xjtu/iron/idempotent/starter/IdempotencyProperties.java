package com.xjtu.iron.idempotent.starter;

import com.xjtu.iron.idempotent.api.IdempotencyMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 分布式幂等组件语义配置。
 *
 * <p>这里只配置组件行为，不复制基础设施连接信息：</p>
 * <ul>
 *     <li>Redis 连接继续使用 {@code spring.data.redis.*}</li>
 *     <li>数据库连接继续使用 {@code spring.datasource.*}</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "xjtu.iron.idempotent")
public class IdempotencyProperties {

    private boolean enabled = true;
    private IdempotencyMode defaultMode = IdempotencyMode.DURABLE;
    private String defaultShortTermRepository = "redis";
    private String defaultDurableRepository = "jdbc";
    private Duration processingTimeout = Duration.ofSeconds(30);
    private Duration shortTermRecordTtl = Duration.ofMinutes(10);
    private boolean retryOnProcessingTimeout = true;
    private boolean retryFailed = true;
    private boolean storeResult = false;

    private final Lock lock = new Lock();
    private final Redis redis = new Redis();
    private final Jdbc jdbc = new Jdbc();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public IdempotencyMode getDefaultMode() {
        return defaultMode;
    }

    public void setDefaultMode(IdempotencyMode defaultMode) {
        this.defaultMode = defaultMode;
    }

    public String getDefaultShortTermRepository() {
        return defaultShortTermRepository;
    }

    public void setDefaultShortTermRepository(String value) {
        this.defaultShortTermRepository = value;
    }

    public String getDefaultDurableRepository() {
        return defaultDurableRepository;
    }

    public void setDefaultDurableRepository(String value) {
        this.defaultDurableRepository = value;
    }

    public Duration getProcessingTimeout() {
        return processingTimeout;
    }

    public void setProcessingTimeout(Duration processingTimeout) {
        this.processingTimeout = processingTimeout;
    }

    public Duration getShortTermRecordTtl() {
        return shortTermRecordTtl;
    }

    public void setShortTermRecordTtl(Duration shortTermRecordTtl) {
        this.shortTermRecordTtl = shortTermRecordTtl;
    }

    public boolean isRetryOnProcessingTimeout() {
        return retryOnProcessingTimeout;
    }

    public void setRetryOnProcessingTimeout(boolean value) {
        this.retryOnProcessingTimeout = value;
    }

    public boolean isRetryFailed() {
        return retryFailed;
    }

    public void setRetryFailed(boolean retryFailed) {
        this.retryFailed = retryFailed;
    }

    public boolean isStoreResult() {
        return storeResult;
    }

    public void setStoreResult(boolean storeResult) {
        this.storeResult = storeResult;
    }

    public Lock getLock() {
        return lock;
    }

    public Redis getRedis() {
        return redis;
    }

    public Jdbc getJdbc() {
        return jdbc;
    }

    /** 可选的 DistributedLockClient 协调参数。 */
    public static class Lock {
        private boolean enabled = false;
        private String providerName;
        private Duration waitTime = Duration.ZERO;
        private Duration leaseTime = Duration.ofSeconds(5);
        private boolean fallbackToStateOnFailure = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getProviderName() {
            return providerName;
        }

        public void setProviderName(String providerName) {
            this.providerName = providerName;
        }

        public Duration getWaitTime() {
            return waitTime;
        }

        public void setWaitTime(Duration waitTime) {
            this.waitTime = waitTime;
        }

        public Duration getLeaseTime() {
            return leaseTime;
        }

        public void setLeaseTime(Duration leaseTime) {
            this.leaseTime = leaseTime;
        }

        public boolean isFallbackToStateOnFailure() {
            return fallbackToStateOnFailure;
        }

        public void setFallbackToStateOnFailure(boolean value) {
            this.fallbackToStateOnFailure = value;
        }
    }

    /** SHORT_TERM Redis Repository 参数。 */
    public static class Redis {
        private boolean enabled = true;
        private String keyPrefix = "iron:idempotency";

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

    /** DURABLE JDBC Repository 参数。 */
    public static class Jdbc {
        private boolean enabled = true;
        private String tableName = "iron_idempotency_record";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }
    }
}
