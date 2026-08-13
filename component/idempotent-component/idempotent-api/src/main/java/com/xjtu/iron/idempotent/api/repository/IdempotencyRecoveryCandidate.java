package com.xjtu.iron.idempotent.api.repository;

import com.xjtu.iron.idempotent.api.IdempotencyStatus;

import java.time.Instant;

/** Reliable Task 可持久化/投递的恢复候选快照。 */
public final class IdempotencyRecoveryCandidate {

    private final String namespace;
    private final String key;
    private final String routeKey;
    private final String requestHash;
    private final IdempotencyStatus status;
    private final String ownerToken;
    private final long version;
    private final Instant processingExpireAt;
    private final String failureCode;

    public IdempotencyRecoveryCandidate(
            String namespace,
            String key,
            String routeKey,
            String requestHash,
            IdempotencyStatus status,
            String ownerToken,
            long version,
            Instant processingExpireAt,
            String failureCode) {
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

    public String getNamespace() { return namespace; }
    public String getKey() { return key; }
    public String getRouteKey() { return routeKey; }
    public String getRequestHash() { return requestHash; }
    public IdempotencyStatus getStatus() { return status; }
    public String getOwnerToken() { return ownerToken; }
    public long getVersion() { return version; }
    public Instant getProcessingExpireAt() { return processingExpireAt; }
    public String getFailureCode() { return failureCode; }
}
