package com.xjtu.iron.idempotent.api;

import java.time.Instant;

/**
 * 业务 callback 获取到的本次执行权上下文。
 *
 * <p>{@code version} 每次 FAILED/过期 PROCESSING 被重新抢占都会递增。
 * Repository 使用 ownerToken + version 防止旧 owner 修改幂等记录；对于更高风险的业务，
 * 业务表也可以使用 {@link #fencingVersion()} 做条件更新，拒绝恢复后的旧执行者。</p>
 */
public final class IdempotencyContext {

    private final String namespace;
    private final String key;
    private final String ownerToken;
    private final long version;
    private final IdempotencyMode mode;
    private final Instant acquiredAt;
    private final Instant processingExpireAt;

    public IdempotencyContext(
            String namespace,
            String key,
            String ownerToken,
            long version,
            IdempotencyMode mode,
            Instant acquiredAt,
            Instant processingExpireAt) {
        this.namespace = namespace;
        this.key = key;
        this.ownerToken = ownerToken;
        this.version = version;
        this.mode = mode;
        this.acquiredAt = acquiredAt;
        this.processingExpireAt = processingExpireAt;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getKey() {
        return key;
    }

    public String getOwnerToken() {
        return ownerToken;
    }

    public long getVersion() {
        return version;
    }

    /**
     * 语义化别名：业务资源需要 fencing 时优先使用这个名字。
     */
    public long fencingVersion() {
        return version;
    }

    public IdempotencyMode getMode() {
        return mode;
    }

    public Instant getAcquiredAt() {
        return acquiredAt;
    }

    public Instant getProcessingExpireAt() {
        return processingExpireAt;
    }
}
