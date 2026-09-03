package com.xjtu.iron.idempotent.api.repository.recovery;

import com.xjtu.iron.idempotent.api.state.IdempotencyStatus;
import com.xjtu.iron.idempotent.api.storage.IdempotencyStorageContext;

import java.time.Instant;

/** Reliable Task 可持久化/投递的恢复候选快照。 */
public final class IdempotencyRecoveryCandidate {

    private final String storeName;
    private final long shardKey;
    private final int scanBucket;
    private final String namespace;
    private final String key;
    private final String routeKey;
    private final String requestHash;
    private final IdempotencyStatus status;
    private final String ownerToken;
    private final long version;
    private final Instant processingExpireAt;
    private final String failureCode;

    public IdempotencyRecoveryCandidate(String storeName, long shardKey, int scanBucket, String namespace, String key, String routeKey,
                                        String requestHash, IdempotencyStatus status, String ownerToken, long version,
                                        Instant processingExpireAt, String failureCode) {
        this.storeName = storeName;
        this.shardKey = shardKey;
        this.scanBucket = scanBucket;
        this.namespace = namespace;
        this.key = key;
        this.routeKey = routeKey;
        this.requestHash = requestHash;
        this.status = status;
        this.ownerToken = ownerToken;
        this.version = version;
        this.processingExpireAt = processingExpireAt;
        this.failureCode = failureCode;
    }

    public String getStoreName() { return storeName; }
    public long getShardKey() { return shardKey; }
    public int getScanBucket() { return scanBucket; }
    public String getNamespace() { return namespace; }
    public String getKey() { return key; }
    public String getRouteKey() { return routeKey; }
    public String getRequestHash() { return requestHash; }
    public IdempotencyStatus getStatus() { return status; }
    public String getOwnerToken() { return ownerToken; }
    public long getVersion() { return version; }
    public Instant getProcessingExpireAt() { return processingExpireAt; }
    public String getFailureCode() { return failureCode; }

    public IdempotencyStorageContext storageContext() {
        return IdempotencyStorageContext.of(storeName, shardKey, scanBucket);
    }
}
