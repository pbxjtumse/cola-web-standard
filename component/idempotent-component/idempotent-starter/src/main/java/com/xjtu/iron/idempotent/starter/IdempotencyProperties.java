package com.xjtu.iron.idempotent.starter;

import com.xjtu.iron.idempotent.api.execution.*;
import com.xjtu.iron.idempotent.api.policy.*;
import com.xjtu.iron.idempotent.api.recovery.*;
import com.xjtu.iron.idempotent.api.state.*;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 分布式幂等组件配置。
 *
 * <p>组件级提供 WINDOWED / DURABLE 默认策略，并支持通过 policies 定义命名 Policy。</p>
 */
@ConfigurationProperties(prefix = "xjtu.iron.idempotent")
public class IdempotencyProperties {

    private boolean enabled = true;

    /** 未在请求中指定 policyName/inline policy 时使用的默认命名策略。 */
    private String defaultPolicy = "durable-default";

    private String defaultWindowedRepository = "redis";
    private String defaultDurableRepository = "jdbc";
    private Duration processingTimeout = Duration.ofSeconds(30);

    private final Windowed windowed = new Windowed();
    private final Durable durable = new Durable();
    private final Lock lock = new Lock();
    private final Transaction transaction = new Transaction();
    private final Redis redis = new Redis();
    private final Jdbc jdbc = new Jdbc();

    /** 用户自定义命名策略。 */
    private final Map<String, Policy> policies = new LinkedHashMap<>();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getDefaultPolicy() { return defaultPolicy; }
    public void setDefaultPolicy(String defaultPolicy) { this.defaultPolicy = defaultPolicy; }
    public String getDefaultWindowedRepository() { return defaultWindowedRepository; }
    public void setDefaultWindowedRepository(String value) { this.defaultWindowedRepository = value; }
    public String getDefaultDurableRepository() { return defaultDurableRepository; }
    public void setDefaultDurableRepository(String value) { this.defaultDurableRepository = value; }
    public Duration getProcessingTimeout() { return processingTimeout; }
    public void setProcessingTimeout(Duration processingTimeout) { this.processingTimeout = processingTimeout; }
    public Windowed getWindowed() { return windowed; }

    public Durable getDurable() { return durable; }
    public Lock getLock() { return lock; }
    public Transaction getTransaction() { return transaction; }
    public Redis getRedis() { return redis; }
    public Jdbc getJdbc() { return jdbc; }
    public Map<String, Policy> getPolicies() { return policies; }

    public static class Windowed {
        private Duration idempotencyWindow = Duration.ofMinutes(10);
        private IdempotencyWindowPolicy windowPolicy =
                IdempotencyWindowPolicy.FIXED_FROM_FIRST_ACQUIRE;
        private Duration recordRetentionTtl = Duration.ZERO;
        private IdempotencyRecoveryMode recoveryMode = IdempotencyRecoveryMode.NONE;
        private boolean recoverProcessingTimeout;
        private boolean recoverFailed;

        public Duration getIdempotencyWindow() { return idempotencyWindow; }
        public void setIdempotencyWindow(Duration value) { this.idempotencyWindow = value; }
        public IdempotencyWindowPolicy getWindowPolicy() { return windowPolicy; }
        public void setWindowPolicy(IdempotencyWindowPolicy value) { this.windowPolicy = value; }
        public Duration getRecordRetentionTtl() { return recordRetentionTtl; }
        public void setRecordRetentionTtl(Duration value) { this.recordRetentionTtl = value; }
        public IdempotencyRecoveryMode getRecoveryMode() { return recoveryMode; }
        public void setRecoveryMode(IdempotencyRecoveryMode value) { this.recoveryMode = value; }
        public boolean isRecoverProcessingTimeout() { return recoverProcessingTimeout; }
        public void setRecoverProcessingTimeout(boolean value) { this.recoverProcessingTimeout = value; }
        public boolean isRecoverFailed() { return recoverFailed; }
        public void setRecoverFailed(boolean value) { this.recoverFailed = value; }
    }

    public static class Durable {
        private IdempotencyRecoveryMode recoveryMode = IdempotencyRecoveryMode.EXTERNAL_TASK;
        private boolean recoverProcessingTimeout = true;
        private boolean recoverFailed = true;

        public IdempotencyRecoveryMode getRecoveryMode() { return recoveryMode; }
        public void setRecoveryMode(IdempotencyRecoveryMode value) { this.recoveryMode = value; }
        public boolean isRecoverProcessingTimeout() { return recoverProcessingTimeout; }
        public void setRecoverProcessingTimeout(boolean value) { this.recoverProcessingTimeout = value; }
        public boolean isRecoverFailed() { return recoverFailed; }
        public void setRecoverFailed(boolean value) { this.recoverFailed = value; }
    }

    /** 全局默认短锁配置；命名 Policy 可以选择是否覆盖。 */
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

    public static class Transaction {
        private boolean enabled = true;
        private boolean requireTemplate = false;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isRequireTemplate() { return requireTemplate; }
        public void setRequireTemplate(boolean requireTemplate) { this.requireTemplate = requireTemplate; }
    }

    /**
     * 自定义命名 Policy。
     *
     * <p>null 字段会继承组件默认值；lockEnabled=null 表示继承全局 lock.enabled。</p>
     */
    public static class Policy {
        private IdempotencyMode mode;
        private String namespace = IdempotencyPolicy.DEFAULT_NAMESPACE;
        private String repositoryName;
        private Duration processingTimeout;
        private Duration idempotencyWindow;
        private IdempotencyWindowPolicy windowPolicy;
        private Duration recordRetentionTtl;
        private IdempotencyRecoveryMode recoveryMode;
        private Boolean recoverProcessingTimeout;
        private Boolean recoverFailed;

        private Boolean lockEnabled;
        private String lockProviderName;
        private Duration lockWaitTime;
        private Duration lockLeaseTime;
        private Boolean lockFallbackToStateOnFailure;

        public IdempotencyMode getMode() { return mode; }
        public void setMode(IdempotencyMode mode) { this.mode = mode; }
        public String getNamespace() { return namespace; }
        public void setNamespace(String namespace) { this.namespace = namespace; }
        public String getRepositoryName() { return repositoryName; }
        public void setRepositoryName(String repositoryName) { this.repositoryName = repositoryName; }
        public Duration getProcessingTimeout() { return processingTimeout; }
        public void setProcessingTimeout(Duration processingTimeout) { this.processingTimeout = processingTimeout; }
        public Duration getIdempotencyWindow() { return idempotencyWindow; }
        public void setIdempotencyWindow(Duration idempotencyWindow) { this.idempotencyWindow = idempotencyWindow; }
        public IdempotencyWindowPolicy getWindowPolicy() { return windowPolicy; }
        public void setWindowPolicy(IdempotencyWindowPolicy windowPolicy) { this.windowPolicy = windowPolicy; }
        public Duration getRecordRetentionTtl() { return recordRetentionTtl; }
        public void setRecordRetentionTtl(Duration recordRetentionTtl) { this.recordRetentionTtl = recordRetentionTtl; }
        public IdempotencyRecoveryMode getRecoveryMode() { return recoveryMode; }
        public void setRecoveryMode(IdempotencyRecoveryMode recoveryMode) { this.recoveryMode = recoveryMode; }
        public Boolean getRecoverProcessingTimeout() { return recoverProcessingTimeout; }
        public void setRecoverProcessingTimeout(Boolean recoverProcessingTimeout) {
            this.recoverProcessingTimeout = recoverProcessingTimeout;
        }
        public Boolean getRecoverFailed() { return recoverFailed; }
        public void setRecoverFailed(Boolean recoverFailed) { this.recoverFailed = recoverFailed; }

        public Boolean getLockEnabled() { return lockEnabled; }
        public void setLockEnabled(Boolean lockEnabled) { this.lockEnabled = lockEnabled; }
        public String getLockProviderName() { return lockProviderName; }
        public void setLockProviderName(String lockProviderName) { this.lockProviderName = lockProviderName; }
        public Duration getLockWaitTime() { return lockWaitTime; }
        public void setLockWaitTime(Duration lockWaitTime) { this.lockWaitTime = lockWaitTime; }
        public Duration getLockLeaseTime() { return lockLeaseTime; }
        public void setLockLeaseTime(Duration lockLeaseTime) { this.lockLeaseTime = lockLeaseTime; }
        public Boolean getLockFallbackToStateOnFailure() { return lockFallbackToStateOnFailure; }
        public void setLockFallbackToStateOnFailure(Boolean value) {
            this.lockFallbackToStateOnFailure = value;
        }
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
