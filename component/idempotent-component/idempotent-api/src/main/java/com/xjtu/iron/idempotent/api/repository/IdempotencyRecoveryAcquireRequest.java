package com.xjtu.iron.idempotent.api.repository;

import com.xjtu.iron.idempotent.api.IdempotencyMode;

import java.time.Duration;
import java.time.Instant;

/** Reliable Task 恢复抢占请求。 */
public final class IdempotencyRecoveryAcquireRequest {

    private final String namespace;
    private final String key;
    private final String requestHash;
    private final String routeKey;
    private final String newOwnerToken;
    private final String expectedOwnerToken;
    private final Long expectedVersion;
    private final IdempotencyMode mode;
    private final Duration processingTimeout;
    private final boolean recoverFailed;
    private final Instant now;

    public IdempotencyRecoveryAcquireRequest(
            String namespace,
            String key,
            String requestHash,
            String routeKey,
            String newOwnerToken,
            String expectedOwnerToken,
            Long expectedVersion,
            IdempotencyMode mode,
            Duration processingTimeout,
            boolean recoverFailed,
            Instant now) {
        this.namespace = namespace;
        this.key = key;
        this.requestHash = requestHash;
        this.routeKey = routeKey;
        this.newOwnerToken = newOwnerToken;
        this.expectedOwnerToken = expectedOwnerToken;
        this.expectedVersion = expectedVersion;
        this.mode = mode;
        this.processingTimeout = processingTimeout;
        this.recoverFailed = recoverFailed;
        this.now = now;
    }

    public String getNamespace() { return namespace; }
    public String getKey() { return key; }
    public String getRequestHash() { return requestHash; }
    public String getRouteKey() { return routeKey; }
    public String getNewOwnerToken() { return newOwnerToken; }
    public String getExpectedOwnerToken() { return expectedOwnerToken; }
    public Long getExpectedVersion() { return expectedVersion; }
    public IdempotencyMode getMode() { return mode; }
    public Duration getProcessingTimeout() { return processingTimeout; }
    public boolean isRecoverFailed() { return recoverFailed; }
    public Instant getNow() { return now; }
}
