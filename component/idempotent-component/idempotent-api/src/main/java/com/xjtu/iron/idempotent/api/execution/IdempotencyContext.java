package com.xjtu.iron.idempotent.api.execution;

import com.xjtu.iron.idempotent.api.policy.IdempotencyMode;
import com.xjtu.iron.idempotent.api.storage.IdempotencyStorageContext;

import java.time.Instant;
import java.util.Objects;

/**
 * 业务 callback 获取到的当前幂等执行权上下文。
 *
 * <p>只有真正抢到 PROCESSING 执行权的调用才会拿到该对象。V2 同时暴露 StorageContext，
 * 使业务/适配层在需要记录诊断信息或向后续任务传递恢复路由时，可以拿到稳定的 storeName/shardKey/scanBucket。</p>
 */
public final class IdempotencyContext {

    private final IdempotencyStorageContext storageContext;
    private final String namespace;
    private final String key;
    private final String routeKey;
    private final String ownerToken;
    private final long version;
    private final IdempotencyMode mode;
    private final boolean recoveryExecution;
    private final Instant acquiredAt;
    private final Instant processingExpireAt;

    public IdempotencyContext(IdempotencyStorageContext storageContext, String namespace, String key, String routeKey, String ownerToken,
                              long version, IdempotencyMode mode, boolean recoveryExecution, Instant acquiredAt,
                              Instant processingExpireAt) {
        this.storageContext = Objects.requireNonNull(storageContext, "storageContext must not be null");
        this.namespace = namespace;
        this.key = key;
        this.routeKey = routeKey;
        this.ownerToken = ownerToken;
        this.version = version;
        this.mode = mode;
        this.recoveryExecution = recoveryExecution;
        this.acquiredAt = acquiredAt;
        this.processingExpireAt = processingExpireAt;
    }

    public IdempotencyStorageContext getStorageContext() { return storageContext; }
    public String getStoreName() { return storageContext.getStoreName(); }
    public long getShardKey() { return storageContext.getShardKey(); }
    public int getScanBucket() { return storageContext.getScanBucket(); }
    public String getNamespace() { return namespace; }
    public String getKey() { return key; }
    public String getRouteKey() { return routeKey; }
    public String getOwnerToken() { return ownerToken; }
    public long getVersion() { return version; }

    /**
     * 当前幂等 generation 的版本号。它用于幂等记录自身的 owner/version CAS，不能默认等同于 distributed-lock fencingToken。
     */
    public long getGenerationVersion() { return version; }

    public IdempotencyMode getMode() { return mode; }
    public boolean isRecoveryExecution() { return recoveryExecution; }
    public Instant getAcquiredAt() { return acquiredAt; }
    public Instant getProcessingExpireAt() { return processingExpireAt; }
}
