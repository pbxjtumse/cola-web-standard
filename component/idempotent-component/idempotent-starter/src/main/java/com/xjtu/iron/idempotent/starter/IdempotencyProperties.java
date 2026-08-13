package com.xjtu.iron.idempotent.starter;

import com.xjtu.iron.idempotent.api.IdempotencyMode;
import com.xjtu.iron.idempotent.api.IdempotencyRecoveryMode;
import com.xjtu.iron.idempotent.api.IdempotencyWindowPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 分布式幂等组件语义配置。
 *
 * <p>基础设施连接仍由应用统一提供：</p>
 * <ul>
 *     <li>Redis：spring.data.redis.*</li>
 *     <li>DataSource：spring.datasource.*</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "xjtu.iron.idempotent")
public class IdempotencyProperties {

    private boolean enabled = true;
    private IdempotencyMode defaultMode = IdempotencyMode.DURABLE;
    private String defaultShortTermRepository = "redis";
    private String defaultDurableRepository = "jdbc";
    private Duration processingTimeout = Duration.ofSeconds(30);
    private boolean storeResult = false;

    private final ShortTerm shortTerm = new ShortTerm();
    private final Durable durable = new Durable();
    private final Lock lock = new Lock();
    private final Redis redis = new Redis();
    private final Jdbc jdbc = new Jdbc();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public IdempotencyMode getDefaultMode() { return defaultMode; }
    public void setDefaultMode(IdempotencyMode defaultMode) { this.defaultMode = defaultMode; }
    public String getDefaultShortTermRepository() { return defaultShortTermRepository; }
    public void setDefaultShortTermRepository(String value) { this.defaultShortTermRepository = value; }
    public String getDefaultDurableRepository() { return defaultDurableRepository; }
    public void setDefaultDurableRepository(String value) { this.defaultDurableRepository = value; }
    public Duration getProcessingTimeout() { return processingTimeout; }
    public void setProcessingTimeout(Duration processingTimeout) { this.processingTimeout = processingTimeout; }
    public boolean isStoreResult() { return storeResult; }
    public void setStoreResult(boolean storeResult) { this.storeResult = storeResult; }
    public ShortTerm getShortTerm() { return shortTerm; }
    public Durable getDurable() { return durable; }
    public Lock getLock() { return lock; }
    public Redis getRedis() { return redis; }
    public Jdbc getJdbc() { return jdbc; }

    /** SHORT_TERM 语义。 */
    public static class ShortTerm {
        private Duration idempotencyWindow = Duration.ofMinutes(10);
        private IdempotencyWindowPolicy windowPolicy = IdempotencyWindowPolicy.FIXED_FROM_FIRST_ACQUIRE;
        private Duration recordRetentionTtl = Duration.ZERO;
        private IdempotencyRecoveryMode recoveryMode = IdempotencyRecoveryMode.NONE;

        public Duration getIdempotencyWindow() { return idempotencyWindow; }
        public void setIdempotencyWindow(Duration value) { this.idempotencyWindow = value; }
        public IdempotencyWindowPolicy getWindowPolicy() { return windowPolicy; }
        public void setWindowPolicy(IdempotencyWindowPolicy value) { this.windowPolicy = value; }
        public Duration getRecordRetentionTtl() { return recordRetentionTtl; }
        public void setRecordRetentionTtl(Duration value) { this.recordRetentionTtl = value; }
        public IdempotencyRecoveryMode getRecoveryMode() { return recoveryMode; }
        public void setRecoveryMode(IdempotencyRecoveryMode value) { this.recoveryMode = value; }
    }

    /** DURABLE 恢复语义。 */
    public static class Durable {
        private IdempotencyRecoveryMode recoveryMode = IdempotencyRecoveryMode.EXTERNAL_TASK;
        private boolean recoverFailed = true;

        public IdempotencyRecoveryMode getRecoveryMode() { return recoveryMode; }
        public void setRecoveryMode(IdempotencyRecoveryMode value) { this.recoveryMode = value; }
        public boolean isRecoverFailed() { return recoverFailed; }
        public void setRecoverFailed(boolean value) { this.recoverFailed = value; }
    }

    /** 可选 DistributedLockClient 协调参数。 */
    public static class Lock {
        private boolean enabled = false;
        private String providerName;
        private Duration waitTime = Duration.ZERO;
        private Duration leaseTime = Duration.ofSeconds(5);
        private boolean fallbackToStateOnFailure = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getProviderName() { return providerName; }
        public void setProviderName(String providerName) { this.providerName = providerName; }
        public Duration getWaitTime() { return waitTime; }
        public void setWaitTime(Duration waitTime) { this.waitTime = waitTime; }
        public Duration getLeaseTime() { return leaseTime; }
        public void setLeaseTime(Duration leaseTime) { this.leaseTime = leaseTime; }
        public boolean isFallbackToStateOnFailure() { return fallbackToStateOnFailure; }
        public void setFallbackToStateOnFailure(boolean value) { this.fallbackToStateOnFailure = value; }
    }

    public static class Redis {
        private boolean enabled = true;
        private String keyPrefix = "iron:idempotency";
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getKeyPrefix() { return keyPrefix; }
        public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }
    }

    public static class Jdbc {
        private boolean enabled = true;
        private String tableName = "iron_idempotency_record";
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
    }
}
